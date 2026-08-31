package com.wakeupalarm.android.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm, timeInMillis: Long? = null) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scheduledTime = timeInMillis ?: calculateNextTime(alarm)

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            scheduledTime,
            pendingIntent
        )
    }

    private fun calculateNextTime(alarm: Alarm): Long {
        val now = LocalDateTime.now()
        var alarmTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(alarm.hour, alarm.minute))

        if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        if (alarm.repeatDays.isNotEmpty()) {
            val nextRepeat = findNextRepeatDay(alarmTime, alarm.repeatDays)
            alarmTime = nextRepeat
        }
        
        return alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun findNextRepeatDay(startTime: LocalDateTime, repeatDays: Set<Int>): LocalDateTime {
        var current = startTime
        // repeatDays: 1=Mon, ..., 7=Sun (matching java.time.DayOfWeek)
        while (!repeatDays.contains(current.dayOfWeek.value)) {
            current = current.plusDays(1)
        }
        return current
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
        }
    }
}
