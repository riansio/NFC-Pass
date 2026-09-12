package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromCardType(value: CardType?): String {
        return value?.name ?: CardType.ACCESS_BADGE.name
    }

    @TypeConverter
    fun toCardType(value: String?): CardType {
        return CardType.fromString(value)
    }
}
