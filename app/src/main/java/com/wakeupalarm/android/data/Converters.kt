package com.wakeupalarm.android.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntSet(set: Set<Int>): String {
        return set.joinToString(",")
    }

    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        if (value.isEmpty()) return emptySet()
        return value.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
