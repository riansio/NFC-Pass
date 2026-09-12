package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "nfc_cards")
data class NfcCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val facilityName: String = "Virtual Facility",
    val cardType: CardType = CardType.ACCESS_BADGE,
    val category: String = "Office & Work",
    val description: String = "",
    val uidHex: String,
    val atqaHex: String = "00:04",
    val sakHex: String = "08",
    val techList: List<String> = listOf("android.nfc.tech.NfcA", "android.nfc.tech.IsoDep"),
    val historicalBytesHex: String = "80:4F:0C:A0:00:00:03:06",
    val ndefPayload: String = "",
    val ndefMimeOrUri: String = "",
    val facilityCode: String = "",
    val cardNumber: String = "",
    val colorGradientIndex: Int = 0,
    val isActiveVirtualCard: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastEmulatedAt: Long? = null,
    val timesEmulated: Int = 0,
    val notes: String = ""
)
