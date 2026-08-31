package com.wakeupalarm.android

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.ui.AlarmViewModel
import com.wakeupalarm.android.ui.components.AlarmListScreen
import com.wakeupalarm.android.ui.theme.WakeupAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeupAlarmTheme {
                val viewModel: AlarmViewModel = viewModel()
                val alarms by viewModel.allAlarms.collectAsState()
                val settings by viewModel.settings.collectAsState()
                
                var showBatteryDialog by remember { mutableStateOf(false) }
                var showAlarmPermissionDialog by remember { mutableStateOf(false) }

                val lifecycleOwner = LocalLifecycleOwner.current
                val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

                LaunchedEffect(lifecycleState) {
                    if (lifecycleState == Lifecycle.State.RESUMED) {
                        val pm = getSystemService(PowerManager::class.java)
                        showBatteryDialog = pm != null && !pm.isIgnoringBatteryOptimizations(packageName)

                        val alarmManager = getSystemService(AlarmManager::class.java)
                        showAlarmPermissionDialog = alarmManager != null && !alarmManager.canScheduleExactAlarms()
                    }
                }

                if (showBatteryDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryDialog = false },
                        title = { Text(stringResource(R.string.dialog_battery_title)) },
                        text = { Text(stringResource(R.string.dialog_battery_message)) },
                        confirmButton = {
                            Button(onClick = {
                                showBatteryDialog = false
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                startActivity(intent)
                            }) {
                                Text(stringResource(R.string.action_go_to_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryDialog = false }) {
                                Text(stringResource(R.string.action_later))
                            }
                        }
                    )
                }

                if (showAlarmPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showAlarmPermissionDialog = false },
                        title = { Text(stringResource(R.string.dialog_alarm_permission_title)) },
                        text = { Text(stringResource(R.string.dialog_alarm_permission_message)) },
                        confirmButton = {
                            Button(onClick = {
                                showAlarmPermissionDialog = false
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
                                startActivity(intent)
                            }) {
                                Text(stringResource(R.string.action_grant_permission))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAlarmPermissionDialog = false }) {
                                Text(stringResource(R.string.action_later))
                            }
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                val musicPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { selectedUri ->
                        viewModel.updateSettings(settings.copy(globalMusicUri = selectedUri.toString()))
                    }
                }

                AlarmListScreen(
                    alarms = alarms,
                    settings = settings,
                    onAddAlarm = {
                        viewModel.addAlarm(Alarm(hour = 8, minute = 0))
                    },
                    onToggleAlarm = { viewModel.toggleAlarm(it) },
                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                    onUpdateAlarm = { viewModel.updateAlarm(it) },
                    onPickMusic = {
                        musicPickerLauncher.launch("audio/*")
                    },
                    onUpdateSettings = { viewModel.updateSettings(it) }
                )
            }
        }
    }
}
