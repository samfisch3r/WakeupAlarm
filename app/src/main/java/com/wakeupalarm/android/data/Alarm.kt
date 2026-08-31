package com.wakeupalarm.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(), // 1 for Monday, ..., 7 for Sunday
    val volume: Int = 70, // 0-100
    val currentSnoozeCount: Int = 0,
    val vibrate: Boolean = true
)
