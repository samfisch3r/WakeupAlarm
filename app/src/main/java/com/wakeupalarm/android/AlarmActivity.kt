package com.wakeupalarm.android

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeupalarm.android.service.AlarmService
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
                        viewModel.dismiss(alarmId)
                        stopService(Intent(this, AlarmService::class.java))
                        finish()
                    },
                    onSnooze = {
                        viewModel.snooze(alarmId)
                        stopService(Intent(this, AlarmService::class.java))
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
