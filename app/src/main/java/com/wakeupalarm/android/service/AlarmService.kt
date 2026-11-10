package com.wakeupalarm.android.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import java.time.Instant
import com.wakeupalarm.android.AlarmActivity
import com.wakeupalarm.android.R
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.data.AlarmDatabase
import com.wakeupalarm.android.data.Settings
import com.wakeupalarm.android.receiver.AlarmReceiver
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
    private val channelId = "alarm_channel"
    private val notificationId = 1001

    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private var currentAlarmId: Int = -1

    private val autoDismissRunnable = Runnable {
        if (currentAlarmId != -1) {
            val dismissIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                putExtra("ALARM_ID", currentAlarmId)
            }
            sendBroadcast(dismissIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        currentAlarmId = alarmId
        
        val initialNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
            
        startForeground(notificationId, initialNotification)

        if (alarmId != -1) {
            startAlarm(alarmId)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm Notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    private fun startAlarm(alarmId: Int) {
        serviceScope.launch {
            val db = AlarmDatabase.getInstance(applicationContext)
            val alarm = db.alarmDao().getAlarmById(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }

            val settings = db.alarmDao().getSettings().firstOrNull() ?: Settings()

            if (settings.vacationModeEnabled && alarm.repeatDays.isNotEmpty()) {
                val now = Instant.now().toEpochMilli()
                val start = settings.vacationStartDate ?: 0L
                val end = (settings.vacationEndDate ?: 0L) + 86_399_999L
                if (now in start..end) {
                    AlarmScheduler(applicationContext).schedule(alarm)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
            }

            autoDismissHandler.removeCallbacks(autoDismissRunnable)
            autoDismissHandler.postDelayed(autoDismissRunnable, 10 * 60 * 1000L)

            showNotificationAndLaunchActivity(alarm, settings)
            playMusic(settings)
            if (settings.globalVibrate) startVibration()
        }
    }

    @SuppressLint("UseFullScreenIntent")
    private fun showNotificationAndLaunchActivity(alarm: Alarm, settings: Settings) {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra("ALARM_ID", alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id * 10 + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra("ALARM_ID", alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canSnooze = alarm.currentSnoozeCount < settings.defaultMaxSnoozes

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(0, getString(R.string.btn_dismiss), dismissPendingIntent)

        if (canSnooze) {
            builder.addAction(0, getString(R.string.btn_snooze), snoozePendingIntent)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(notificationId, builder.build())

        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playMusic(settings: Settings) {
        val maxVolume = (settings.globalVolume / 100f).coerceIn(0f, 1f)
        val duration = settings.gradualIncreaseSeconds * 1000L
        val interval = 500L
        val steps = if (duration > 0) duration / interval else 0

        val initialVolume = if (steps > 0) (1.toFloat() / steps) * maxVolume else maxVolume

        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        
        val customUri = settings.globalMusicUri?.toUri()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                var setSuccess = false
                if (customUri != null) {
                    try {
                        setDataSource(applicationContext, customUri)
                        setSuccess = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!setSuccess && defaultUri != null) {
                    reset()
                    setDataSource(applicationContext, defaultUri)
                }

                isLooping = true
                setVolume(initialVolume, initialVolume)
                prepare()
                start()
            }

            if (steps > 0) {
                serviceScope.launch {
                    for (i in 1..steps) {
                        val currentVol = (i.toFloat() / steps) * maxVolume
                        mediaPlayer?.setVolume(currentVol, currentVol)
                        delay(interval)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        val vibratorManager = getSystemService(VibratorManager::class.java)
        vibrator = vibratorManager?.defaultVibrator ?: @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as? Vibrator)
        val pattern = longArrayOf(0, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    override fun onDestroy() {
        super.onDestroy()
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrator = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
