package com.wakeupalarm.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakeupalarm.android.data.AlarmDatabase
import com.wakeupalarm.android.data.AlarmRepository
import com.wakeupalarm.android.data.Settings
import com.wakeupalarm.android.service.AlarmService
import com.wakeupalarm.android.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return

        when (intent.action) {
            ACTION_DISMISS -> {
                context.stopService(Intent(context, AlarmService::class.java))
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AlarmDatabase.getInstance(context)
                        val repository = AlarmRepository(db.alarmDao())
                        val alarm = repository.getAlarmById(alarmId)
                        if (alarm != null) {
                            val updated = alarm.copy(currentSnoozeCount = 0)
                            if (alarm.repeatDays.isNotEmpty()) {
                                repository.updateAlarm(updated)
                                AlarmScheduler(context).schedule(updated)
                            } else {
                                repository.updateAlarm(updated.copy(isEnabled = false))
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                context.stopService(Intent(context, AlarmService::class.java))
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AlarmDatabase.getInstance(context)
                        val repository = AlarmRepository(db.alarmDao())
                        val alarm = repository.getAlarmById(alarmId)
                        val settings = db.alarmDao().getSettings().firstOrNull() ?: Settings()
                        if (alarm != null && (alarm.currentSnoozeCount < settings.defaultMaxSnoozes)) {
                            val updated = alarm.copy(currentSnoozeCount = alarm.currentSnoozeCount + 1)
                            repository.updateAlarm(updated)
                            val snoozeTime = (Instant.now() + Duration.ofMinutes(settings.defaultSnoozeMinutes.toLong())).toEpochMilli()
                            AlarmScheduler(context).schedule(updated, snoozeTime)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ALARM_ID", alarmId)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.wakeupalarm.android.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.wakeupalarm.android.ACTION_SNOOZE"
    }
}
