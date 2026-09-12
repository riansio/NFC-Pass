package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcCardDao {
    @Query("SELECT * FROM nfc_cards ORDER BY isActiveVirtualCard DESC, createdAt DESC")
    fun getAllCards(): Flow<List<NfcCard>>

    @Query("SELECT * FROM nfc_cards WHERE isActiveVirtualCard = 1 LIMIT 1")
    fun getActiveCard(): Flow<NfcCard?>

    @Query("SELECT * FROM nfc_cards WHERE isActiveVirtualCard = 1 LIMIT 1")
    suspend fun getActiveCardDirect(): NfcCard?

    @Query("SELECT * FROM nfc_cards WHERE id = :id LIMIT 1")
    fun getCardById(id: Long): Flow<NfcCard?>

    @Query("SELECT * FROM nfc_cards WHERE id = :id LIMIT 1")
    suspend fun getCardByIdDirect(id: Long): NfcCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: NfcCard): Long

    @Update
    suspend fun update(card: NfcCard)

    @Delete
    suspend fun delete(card: NfcCard)

    @Query("DELETE FROM nfc_cards")
    suspend fun deleteAll()

    @Query("UPDATE nfc_cards SET isActiveVirtualCard = 0")
    suspend fun deactivateAllCards()

    @Query("UPDATE nfc_cards SET isActiveVirtualCard = 1 WHERE id = :cardId")
    suspend fun activateCard(cardId: Long)

    @Transaction
    suspend fun setActiveCard(cardId: Long) {
        deactivateAllCards()
        activateCard(cardId)
    }

    @Query("UPDATE nfc_cards SET lastEmulatedAt = :timestamp, timesEmulated = timesEmulated + 1 WHERE id = :cardId")
    suspend fun recordEmulation(cardId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM nfc_cards")
    suspend fun getCount(): Int
}
