package com.example.data

import kotlinx.coroutines.flow.Flow

class CardRepository(private val dao: NfcCardDao) {
    val allCards: Flow<List<NfcCard>> = dao.getAllCards()
    val activeCard: Flow<NfcCard?> = dao.getActiveCard()

    fun getCardById(id: Long): Flow<NfcCard?> = dao.getCardById(id)

    suspend fun getActiveCardDirect(): NfcCard? = dao.getActiveCardDirect()

    suspend fun insert(card: NfcCard): Long = dao.insert(card)

    suspend fun update(card: NfcCard) = dao.update(card)

    suspend fun delete(card: NfcCard) = dao.delete(card)

    suspend fun clearAllCards() = dao.deleteAll()

    suspend fun setActiveCard(cardId: Long) = dao.setActiveCard(cardId)

    suspend fun deactivateAll() = dao.deactivateAllCards()

    suspend fun recordEmulation(cardId: Long) {
        dao.recordEmulation(cardId, System.currentTimeMillis())
    }
}
