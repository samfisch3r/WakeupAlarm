package com.wakeupalarm.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.data.AlarmDatabase
import com.wakeupalarm.android.data.AlarmRepository
import com.wakeupalarm.android.data.Settings
import com.wakeupalarm.android.util.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository
    val allAlarms: StateFlow<List<Alarm>>
    val settings: StateFlow<Settings>
    private val scheduler: AlarmScheduler

    init {
        val alarmDao = AlarmDatabase.getInstance(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        allAlarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        settings = repository.settings
            .map { it ?: Settings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Settings()
            )
        scheduler = AlarmScheduler(application)
    }

    fun updateSettings(newSettings: Settings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val id = repository.insertAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm.copy(id = id.toInt()))
            }
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val isEnabling = !alarm.isEnabled
            val updated = alarm.copy(
                isEnabled = isEnabling,
                currentSnoozeCount = if (isEnabling) 0 else alarm.currentSnoozeCount
            )
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            // Reset snooze count when alarm is updated/re-scheduled
            val updated = alarm.copy(currentSnoozeCount = 0)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            scheduler.cancel(alarm)
        }
    }

    fun dismiss(alarmId: Int) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId) ?: return@launch
            val updated = alarm.copy(currentSnoozeCount = 0)
            
            if (alarm.repeatDays.isNotEmpty()) {
                repository.updateAlarm(updated)
                scheduler.schedule(updated) // Schedule next occurrence
            } else {
                repository.updateAlarm(updated.copy(isEnabled = false))
            }
        }
    }

    fun snooze(alarmId: Int) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId) ?: return@launch
            val currentSettings = settings.value
            if (alarm.currentSnoozeCount < currentSettings.defaultMaxSnoozes) {
                val updated = alarm.copy(currentSnoozeCount = alarm.currentSnoozeCount + 1)
                repository.updateAlarm(updated)
                
                val snoozeTime = Instant.now()
                    .plus(Duration.ofMinutes(currentSettings.defaultSnoozeMinutes.toLong()))
                    .toEpochMilli()
                scheduler.schedule(updated, snoozeTime)
            }
        }
    }
}
