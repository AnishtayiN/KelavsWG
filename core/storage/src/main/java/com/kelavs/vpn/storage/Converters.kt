package com.kelavs.vpn.storage

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return Json.encodeToString(list)
    }
}