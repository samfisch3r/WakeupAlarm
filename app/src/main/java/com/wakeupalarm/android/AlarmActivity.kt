package com.wakeupalarm.android

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeupalarm.android.receiver.AlarmReceiver
import com.wakeupalarm.android.ui.AlarmViewModel
import com.wakeupalarm.android.ui.components.AlarmScreen
import com.wakeupalarm.android.ui.components.calculateNextAlarmInfo
import com.wakeupalarm.android.ui.theme.WakeupAlarmTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupLockScreenFlags()

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        
        setContent {
            WakeupAlarmTheme {
                val viewModel: AlarmViewModel = viewModel()
                val alarms by viewModel.allAlarms.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val alarm = alarms.find { it.id == alarmId }
                val otherAlarms = remember(alarms, alarmId) { alarms.filter { it.id != alarmId } }
                val nextAlarmInfo = remember(otherAlarms, settings) { calculateNextAlarmInfo(otherAlarms, settings) }

                AlarmScreen(
                    alarm = alarm,
                    nextAlarmText = nextAlarmInfo?.displayString,
                    maxSnoozes = settings.defaultMaxSnoozes,
                    onDismiss = {
                        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
                            action = AlarmReceiver.ACTION_DISMISS
                            putExtra("ALARM_ID", alarmId)
                        }
                        sendBroadcast(dismissIntent)
                        finish()
                    },
                    onSnooze = {
                        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                            action = AlarmReceiver.ACTION_SNOOZE
                            putExtra("ALARM_ID", alarmId)
                        }
                        sendBroadcast(snoozeIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        keyguardManager?.requestDismissKeyguard(this, null)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
