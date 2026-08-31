package com.wakeupalarm.android.service

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import java.time.Instant
import com.wakeupalarm.android.AlarmActivity
import com.wakeupalarm.android.R
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.data.AlarmDatabase
import com.wakeupalarm.android.data.Settings
import com.wakeupalarm.android.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        if (alarmId != -1) {
            startAlarm(alarmId)
        }
        return START_STICKY
    }

    private fun startAlarm(alarmId: Int) {
        serviceScope.launch {
            val db = AlarmDatabase.getInstance(applicationContext)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
            val settings = db.alarmDao().getSettings().firstOrNull() ?: Settings()

            if (settings.vacationModeEnabled && alarm.repeatDays.isNotEmpty()) {
                val now = Instant.now().toEpochMilli()
                val start = settings.vacationStartDate ?: 0L
                val end = settings.vacationEndDate ?: 0L
                if (now in start..end) {
                    // Skip ringing for repeating alarms during vacation
                    // Re-schedule for next occurrence
                    AlarmScheduler(applicationContext).schedule(alarm)
                    stopSelf()
                    return@launch
                }
            }

            showNotification(alarm)
            playMusic(alarm, settings)
            if (alarm.vibrate) startVibration()
        }
    }

    private fun showNotification(alarm: Alarm) {
        val channelId = "alarm_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId, 
            getString(R.string.notification_channel_name), 
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager?.createNotificationChannel(channel)

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        startForeground(1, notification)
    }

    private fun playMusic(alarm: Alarm, settings: Settings) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                val uri = settings.globalMusicUri?.let { Uri.parse(it) } 
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
            
            serviceScope.launch {
                val maxVolume = alarm.volume / 100f
                val duration = settings.gradualIncreaseSeconds * 1000L
                val interval = 500L
                val steps = if (duration > 0) duration / interval else 0
                
                if (steps > 0) {
                    for (i in 1..steps) {
                        val currentVol = (i.toFloat() / steps) * maxVolume
                        mediaPlayer?.setVolume(currentVol, currentVol)
                        delay(interval)
                    }
                } else {
                    mediaPlayer?.setVolume(maxVolume, maxVolume)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        val vibratorManager = getSystemService(VibratorManager::class.java)
        vibrator = vibratorManager?.defaultVibrator

        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
