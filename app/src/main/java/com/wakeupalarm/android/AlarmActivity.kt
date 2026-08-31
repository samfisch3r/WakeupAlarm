package com.wakeupalarm.android

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeupalarm.android.service.AlarmService
import com.wakeupalarm.android.ui.AlarmViewModel
import com.wakeupalarm.android.ui.components.AlarmScreen
import com.wakeupalarm.android.ui.theme.WakeupAlarmTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        keyguardManager?.requestDismissKeyguard(this, null)

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        
        setContent {
            WakeupAlarmTheme {
                val viewModel: AlarmViewModel = viewModel()
                val alarms by viewModel.allAlarms.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val alarm = alarms.find { a -> a.id == alarmId }
                
                AlarmScreen(
                    alarm = alarm,
                    maxSnoozes = settings.defaultMaxSnoozes,
                    onDismiss = {
                        viewModel.dismiss(alarmId)
                        stopService(Intent(this, AlarmService::class.java))
                        finish()
                    }
                ) {
                    viewModel.snooze(alarmId)
                    stopService(Intent(this, AlarmService::class.java))
                    finish()
                }
            }
        }
    }
}
