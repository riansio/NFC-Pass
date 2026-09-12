package com.example.data

import android.content.Context
import com.example.R

enum class CardType(val displayName: String, val stringResId: Int) {
    ACCESS_BADGE("Access Badge", R.string.card_type_access_badge),
    MIFARE_CLASSIC("MIFARE Classic (1K/4K)", R.string.card_type_mifare_classic),
    MIFARE_DESFIRE("MIFARE DESFire (EV1/2/3)", R.string.card_type_mifare_desfire),
    MIFARE_ULTRALIGHT("MIFARE Ultralight / NTAG", R.string.card_type_mifare_ultralight),
    DOOR_KEY("Smart Key / Fob", R.string.card_type_door_key),
    HOTEL_KEY("Hotel Keycard", R.string.card_type_hotel_key),
    TRANSIT_PASS("Transit Pass", R.string.card_type_transit_pass),
    FELICA_TRANSIT("FeliCa / Suica / Pasmo", R.string.card_type_felica_transit),
    PAYMENT_CARD("Contactless Card", R.string.card_type_payment_card),
    CAMPUS_STUDENT_ID("Student / Campus ID", R.string.card_type_campus_student_id),
    ISO15693_VICINITY("ISO 15693 Vicinity (SLIX)", R.string.card_type_iso15693_vicinity),
    AMIIBO_GAMING("Amiibo / Gaming Tag", R.string.card_type_amiibo_gaming),
    GOV_EID_PASSPORT("Digital ID / ePassport", R.string.card_type_gov_eid_passport),
    PARKING_TOLL_PASS("Parking & Toll Pass", R.string.card_type_parking_toll_pass),
    GYM_MEMBERSHIP("Gym & Club", R.string.card_type_gym_membership),
    SMART_POSTER("Smart Poster / URL", R.string.card_type_smart_poster),
    GENERIC_TAG("Standard NFC Tag", R.string.card_type_generic_tag);

    fun getLocalizedName(context: Context): String {
        return context.getString(stringResId)
    }

    companion object {
        fun fromString(value: String?): CardType {
            if (value == null) return ACCESS_BADGE
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ACCESS_BADGE
        }
    }
}

