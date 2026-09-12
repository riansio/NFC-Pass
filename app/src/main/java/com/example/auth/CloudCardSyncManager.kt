package com.example.auth

import android.content.Context
import android.util.Log
import com.example.data.CardRepository
import com.example.data.CardType
import com.example.data.NfcCard
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CloudCardSyncManager(
    private val context: Context,
    private val repository: CardRepository
) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore instance not available: ${e.message}")
            null
        }
    }

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    /**
     * Uploads a single newly added or created NFC card to Firestore under the user's account.
     */
    suspend fun syncCardAdded(userId: String, card: NfcCard): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val db = firestore
                if (db == null) {
                    Log.w(TAG, "Firestore not available, queuing local card '${card.name}'")
                    return@withContext Result.success(Unit)
                }

                val cardData = buildCardHashMap(card)
                val docId = getDocId(card)
                db.collection("users")
                    .document(userId)
                    .collection("nfc_cards")
                    .document(docId)
                    .set(cardData, SetOptions.merge())
                    .await()

                Log.i(TAG, "Card '${card.name}' (docId: $docId) successfully synced to account $userId")
                _syncState.value = CloudSyncState.Synced(
                    cardCount = 1,
                    lastSyncedTime = System.currentTimeMillis()
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing added card to account: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Updates an existing NFC card on Firestore under the user's account.
     */
    suspend fun syncCardUpdated(userId: String, card: NfcCard): Result<Unit> {
        return syncCardAdded(userId, card)
    }

    /**
     * Deletes an NFC card from Firestore under the user's account.
     */
    suspend fun syncCardDeleted(userId: String, card: NfcCard): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val db = firestore
                if (db == null) {
                    Log.w(TAG, "Firestore not available, local deletion completed for '${card.name}'")
                    return@withContext Result.success(Unit)
                }

                val userCards = db.collection("users")
                    .document(userId)
                    .collection("nfc_cards")

                val docId = getDocId(card)
                userCards.document(docId).delete().await()

                // Also delete alternate docId format if any existed previously
                if (card.uidHex.isNotBlank()) {
                    val rawUid = card.uidHex.replace(":", "_")
                    if (rawUid != docId) {
                        try { userCards.document(rawUid).delete().await() } catch (_: Exception) {}
                    }
                }
                if (card.id > 0) {
                    try { userCards.document(card.id.toString()).delete().await() } catch (_: Exception) {}
                }

                Log.i(TAG, "Card '${card.name}' (docId: $docId) successfully deleted from account $userId")
                _syncState.value = CloudSyncState.Synced(
                    cardCount = 0,
                    lastSyncedTime = System.currentTimeMillis()
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting card from account: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Full bidirectional sync: pushes all local cards to Firestore,
     * pulls any remote cards not present locally, and reconciles the account.
     */
    suspend fun fullSyncWithAccount(userId: String, localCards: List<NfcCard>): Result<Pair<Int, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = CloudSyncState.Syncing
                val db = firestore ?: return@withContext Result.failure(
                    IllegalStateException("Firestore is not available. Check network & Firebase connection.")
                )

                val userCardsCollection = db.collection("users")
                    .document(userId)
                    .collection("nfc_cards")

                // 1. Upload all local cards
                var uploadedCount = 0
                for (card in localCards) {
                    val cardData = buildCardHashMap(card)
                    val docId = getDocId(card)
                    userCardsCollection.document(docId).set(cardData, SetOptions.merge()).await()
                    uploadedCount++
                }

                // 2. Fetch remote cards and restore any missing locally
                val querySnapshot = userCardsCollection.get().await()
                var restoredCount = 0
                val localUids = localCards.map { it.uidHex.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
                val localNames = localCards.map { it.name.trim().lowercase() }.toSet()

                for (doc in querySnapshot.documents) {
                    val uidHex = doc.getString("uidHex") ?: ""
                    val name = doc.getString("name") ?: continue

                    // If already exists locally by UID or Name, skip duplicate insert
                    val isAlreadyLocal = (uidHex.isNotBlank() && localUids.contains(uidHex.uppercase())) ||
                            localNames.contains(name.lowercase())
                    if (!isAlreadyLocal) {
                        val cardTypeName = doc.getString("cardType") ?: CardType.ACCESS_BADGE.name
                        val cardType = try {
                            CardType.valueOf(cardTypeName)
                        } catch (e: Exception) {
                            CardType.ACCESS_BADGE
                        }

                        @Suppress("UNCHECKED_CAST")
                        val techList = (doc.get("techList") as? List<String>) ?: listOf("android.nfc.tech.NfcA")

                        val card = NfcCard(
                            name = name,
                            facilityName = doc.getString("facilityName") ?: "Cloud Pass",
                            cardType = cardType,
                            category = doc.getString("category") ?: "Office & Work",
                            description = doc.getString("description") ?: "",
                            uidHex = uidHex,
                            atqaHex = doc.getString("atqaHex") ?: "00:04",
                            sakHex = doc.getString("sakHex") ?: "08",
                            techList = techList,
                            historicalBytesHex = doc.getString("historicalBytesHex") ?: "",
                            ndefPayload = doc.getString("ndefPayload") ?: "",
                            ndefMimeOrUri = doc.getString("ndefMimeOrUri") ?: "",
                            facilityCode = doc.getString("facilityCode") ?: "",
                            cardNumber = doc.getString("cardNumber") ?: "",
                            colorGradientIndex = (doc.getLong("colorGradientIndex") ?: 0L).toInt(),
                            notes = doc.getString("notes") ?: "Restored from Google account"
                        )
                        repository.insert(card)
                        restoredCount++
                    }
                }

                val totalCount = localCards.size + restoredCount
                _syncState.value = CloudSyncState.Synced(totalCount, System.currentTimeMillis())
                Log.i(TAG, "Full sync complete: $uploadedCount uploaded, $restoredCount restored for $userId")
                Result.success(Pair(uploadedCount, restoredCount))
            } catch (e: Exception) {
                Log.e(TAG, "Full sync failed: ${e.message}", e)
                _syncState.value = CloudSyncState.Error(e.localizedMessage ?: "Sync failed")
                Result.failure(e)
            }
        }
    }

    private fun getDocId(card: NfcCard): String {
        val cleanUid = card.uidHex.trim().replace(":", "_").replace(" ", "_")
        return if (cleanUid.isNotBlank()) cleanUid else "card_${card.id}"
    }

    private fun buildCardHashMap(card: NfcCard): HashMap<String, Any?> {
        return hashMapOf(
            "id" to card.id,
            "name" to card.name,
            "facilityName" to card.facilityName,
            "cardType" to card.cardType.name,
            "category" to card.category,
            "description" to card.description,
            "uidHex" to card.uidHex,
            "atqaHex" to card.atqaHex,
            "sakHex" to card.sakHex,
            "techList" to card.techList,
            "historicalBytesHex" to card.historicalBytesHex,
            "ndefPayload" to card.ndefPayload,
            "ndefMimeOrUri" to card.ndefMimeOrUri,
            "facilityCode" to card.facilityCode,
            "cardNumber" to card.cardNumber,
            "colorGradientIndex" to card.colorGradientIndex,
            "notes" to card.notes,
            "createdAt" to card.createdAt,
            "lastSyncedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Uploads local NFC cards to Firestore under the user's account:
     * collection: users -> {userId} -> nfc_cards -> {cardUid/id}
     */
    suspend fun backupCardsToAccount(userId: String, localCards: List<NfcCard>): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = CloudSyncState.Syncing
                val db = firestore ?: return@withContext Result.failure(
                    IllegalStateException("Firestore is not available. Check network & Firebase connection.")
                )

                val userCardsCollection = db.collection("users")
                    .document(userId)
                    .collection("nfc_cards")

                var uploadedCount = 0
                for (card in localCards) {
                    val cardData = hashMapOf(
                        "id" to card.id,
                        "name" to card.name,
                        "facilityName" to card.facilityName,
                        "cardType" to card.cardType.name,
                        "category" to card.category,
                        "description" to card.description,
                        "uidHex" to card.uidHex,
                        "atqaHex" to card.atqaHex,
                        "sakHex" to card.sakHex,
                        "techList" to card.techList,
                        "historicalBytesHex" to card.historicalBytesHex,
                        "ndefPayload" to card.ndefPayload,
                        "ndefMimeOrUri" to card.ndefMimeOrUri,
                        "facilityCode" to card.facilityCode,
                        "cardNumber" to card.cardNumber,
                        "colorGradientIndex" to card.colorGradientIndex,
                        "notes" to card.notes,
                        "createdAt" to card.createdAt,
                        "lastSyncedAt" to System.currentTimeMillis()
                    )

                    // Use UID or ID as document key
                    val docId = if (card.uidHex.isNotBlank()) card.uidHex.replace(":", "_") else card.id.toString()
                    userCardsCollection.document(docId).set(cardData, SetOptions.merge()).await()
                    uploadedCount++
                }

                _syncState.value = CloudSyncState.Synced(uploadedCount, System.currentTimeMillis())
                Log.i(TAG, "Backed up $uploadedCount tags to user account $userId")
                Result.success(uploadedCount)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to backup cards to account: ${e.message}", e)
                _syncState.value = CloudSyncState.Error(e.localizedMessage ?: "Cloud sync failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Downloads NFC cards from the user's Firestore account and imports any new ones locally.
     */
    suspend fun restoreCardsFromAccount(userId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = CloudSyncState.Syncing
                val db = firestore ?: return@withContext Result.failure(
                    IllegalStateException("Firestore not available")
                )

                val querySnapshot = db.collection("users")
                    .document(userId)
                    .collection("nfc_cards")
                    .get()
                    .await()

                var importedCount = 0
                for (doc in querySnapshot.documents) {
                    val name = doc.getString("name") ?: continue
                    val uidHex = doc.getString("uidHex") ?: ""
                    val cardTypeName = doc.getString("cardType") ?: CardType.ACCESS_BADGE.name
                    val cardType = try {
                        CardType.valueOf(cardTypeName)
                    } catch (e: Exception) {
                        CardType.ACCESS_BADGE
                    }

                    @Suppress("UNCHECKED_CAST")
                    val techList = (doc.get("techList") as? List<String>) ?: listOf("android.nfc.tech.NfcA")

                    val card = NfcCard(
                        name = name,
                        facilityName = doc.getString("facilityName") ?: "Cloud Pass",
                        cardType = cardType,
                        category = doc.getString("category") ?: "Office & Work",
                        description = doc.getString("description") ?: "",
                        uidHex = uidHex,
                        atqaHex = doc.getString("atqaHex") ?: "00:04",
                        sakHex = doc.getString("sakHex") ?: "08",
                        techList = techList,
                        historicalBytesHex = doc.getString("historicalBytesHex") ?: "",
                        ndefPayload = doc.getString("ndefPayload") ?: "",
                        ndefMimeOrUri = doc.getString("ndefMimeOrUri") ?: "",
                        facilityCode = doc.getString("facilityCode") ?: "",
                        cardNumber = doc.getString("cardNumber") ?: "",
                        colorGradientIndex = (doc.getLong("colorGradientIndex") ?: 0L).toInt(),
                        notes = doc.getString("notes") ?: "Restored from Google account"
                    )

                    repository.insert(card)
                    importedCount++
                }

                _syncState.value = CloudSyncState.Synced(importedCount, System.currentTimeMillis())
                Log.i(TAG, "Restored $importedCount tags from user account $userId")
                Result.success(importedCount)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore cards: ${e.message}", e)
                _syncState.value = CloudSyncState.Error(e.localizedMessage ?: "Restore failed")
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val TAG = "CloudCardSyncManager"
    }
}
