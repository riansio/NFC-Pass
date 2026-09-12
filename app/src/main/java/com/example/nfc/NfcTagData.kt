package com.example.nfc

import com.example.data.CardType

data class NfcTagData(
    val uidHex: String,
    val techList: List<String>,
    val atqaHex: String = "",
    val sakHex: String = "",
    val historicalBytesHex: String = "",
    val ndefPayload: String = "",
    val ndefType: String = "",
    val isWritable: Boolean = true,
    val maxSize: Int = 0,
    val detectedAt: Long = System.currentTimeMillis(),
    val chipTypeGuess: String = "ISO 14443-3A / NFC Forum Tag"
) {
    fun guessCardType(): CardType {
        val payloadUpper = ndefPayload.uppercase()
        val chipUpper = chipTypeGuess.uppercase()

        return when {
            // Payment
            payloadUpper.contains("PAY") || payloadUpper.contains("VISA") || payloadUpper.contains("MASTERCARD") || payloadUpper.contains("EMV") -> CardType.PAYMENT_CARD
            // Transit
            techList.any { it.contains("NfcF", ignoreCase = true) } || chipUpper.contains("FELICA") || payloadUpper.contains("SUICA") || payloadUpper.contains("PASMO") -> CardType.FELICA_TRANSIT
            payloadUpper.contains("NAVIGO") || payloadUpper.contains("CALYPSO") || payloadUpper.contains("TRANSIT") || payloadUpper.contains("METRO") -> CardType.TRANSIT_PASS
            // Gaming / Amiibo
            payloadUpper.contains("AMIIBO") || payloadUpper.contains("NINTENDO") -> CardType.AMIIBO_GAMING
            // Campus / Student
            payloadUpper.contains("STUDENT") || payloadUpper.contains("CAMPUS") || payloadUpper.contains("CBORD") || payloadUpper.contains("UNIVERSITY") -> CardType.CAMPUS_STUDENT_ID
            // Hotel
            payloadUpper.contains("HOTEL") || payloadUpper.contains("ROOM") || payloadUpper.contains("VINGCARD") || payloadUpper.contains("ASSA") -> CardType.HOTEL_KEY
            // Smart lock / Key
            payloadUpper.contains("LOCK") || payloadUpper.contains("GATE") || payloadUpper.contains("DOOR") || payloadUpper.contains("FOB") -> CardType.DOOR_KEY
            // Gym & Fitness
            payloadUpper.contains("GYM") || payloadUpper.contains("FIT") || payloadUpper.contains("CLUB") -> CardType.GYM_MEMBERSHIP
            // Parking & Toll
            payloadUpper.contains("PARK") || payloadUpper.contains("TOLL") || payloadUpper.contains("BARRIER") -> CardType.PARKING_TOLL_PASS
            // ePassport / eID
            payloadUpper.contains("ICAO") || payloadUpper.contains("PASSPORT") || payloadUpper.contains("EID") -> CardType.GOV_EID_PASSPORT
            // Vicinity ISO 15693
            techList.any { it.contains("NfcV", ignoreCase = true) } || chipUpper.contains("15693") || chipUpper.contains("SLIX") -> CardType.ISO15693_VICINITY
            // MIFARE Families
            sakHex.equals("08", ignoreCase = true) || sakHex.equals("18", ignoreCase = true) || chipUpper.contains("CLASSIC") -> CardType.MIFARE_CLASSIC
            sakHex.equals("00", ignoreCase = true) || chipUpper.contains("ULTRALIGHT") || chipUpper.contains("NTAG") -> CardType.MIFARE_ULTRALIGHT
            sakHex.equals("20", ignoreCase = true) || chipUpper.contains("DESFIRE") -> CardType.MIFARE_DESFIRE
            // Smart Poster / URL
            ndefPayload.startsWith("http://", ignoreCase = true) || ndefPayload.startsWith("https://", ignoreCase = true) || ndefPayload.startsWith("WIFI:", ignoreCase = true) -> CardType.SMART_POSTER
            // Access Badge
            techList.any { it.contains("IsoDep", ignoreCase = true) } -> CardType.ACCESS_BADGE
            else -> CardType.ACCESS_BADGE
        }
    }
}
