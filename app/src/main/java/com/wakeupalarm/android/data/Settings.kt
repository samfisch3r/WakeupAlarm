package com.wakeupalarm.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val defaultSnoozeMinutes: Int = 5,
    val defaultMaxSnoozes: Int = 3,
    val globalMusicUri: String? = null,
    val gradualIncreaseSeconds: Int = 60,
    val vacationModeEnabled: Boolean = false,
    val vacationStartDate: Long? = null,
    val vacationEndDate: Long? = null
)
