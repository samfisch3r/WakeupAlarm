package com.wakeupalarm.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeupalarm.android.R
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.data.Settings
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    settings: Settings,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onPickMusic: () -> Unit,
    onUpdateSettings: (Settings) -> Unit,
) {
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_bar_title)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_alarm))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { onToggleAlarm(alarm) },
                    onDelete = { onDeleteAlarm(alarm) },
                    onEdit = { editingAlarm = alarm },
                )
            }
        }

        editingAlarm?.let { alarm ->
            EditAlarmDialog(
                alarm = alarm,
                onDismiss = { editingAlarm = null },
                onConfirm = { updated ->
                    onUpdateAlarm(updated)
                    editingAlarm = null
                },
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings = settings,
                onDismiss = { showSettings = false },
                onConfirm = { updated ->
                    onUpdateSettings(updated)
                    showSettings = false
                },
                onPickMusic = onPickMusic,
            )
        }
    }
}

@Composable
fun SettingsDialog(
    settings: Settings,
    onDismiss: () -> Unit,
    onConfirm: (Settings) -> Unit,
    onPickMusic: () -> Unit,
) {
    var snoozeMinutes by remember { mutableIntStateOf(settings.defaultSnoozeMinutes) }
    var maxSnoozes by remember { mutableIntStateOf(settings.defaultMaxSnoozes) }
    var gradualIncreaseSeconds by remember { mutableIntStateOf(settings.gradualIncreaseSeconds) }
    var vacationEnabled by remember { mutableStateOf(settings.vacationModeEnabled) }
    var vacationStart by remember { mutableStateOf(settings.vacationStartDate) }
    var vacationEnd by remember { mutableStateOf(settings.vacationEndDate) }

    var activePicker by remember { mutableStateOf<String?>(null) } // "snooze", "max", "gradual"
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_global_settings)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Snooze Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePicker = if (activePicker == "snooze") null else "snooze" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.label_snooze), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.label_snooze_mins, snoozeMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (activePicker == "snooze") {
                    WheelPicker(
                        items = (1..60).toList(),
                        initialValue = snoozeMinutes,
                        onValueChange = { snoozeMinutes = it },
                    )
                }

                // Max Snoozes Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePicker = if (activePicker == "max") null else "max" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.label_max_snoozes), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$maxSnoozes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (activePicker == "max") {
                    WheelPicker(
                        items = (1..20).toList(),
                        initialValue = maxSnoozes,
                        onValueChange = { maxSnoozes = it },
                    )
                }

                // Gradual Increase Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePicker = if (activePicker == "gradual") null else "gradual" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.label_gradual_increase), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.label_gradual_increase_secs, gradualIncreaseSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (activePicker == "gradual") {
                    WheelPicker(
                        items = (0..300 step 5).toList(),
                        initialValue = gradualIncreaseSeconds,
                        onValueChange = { gradualIncreaseSeconds = it },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onPickMusic,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (settings.globalMusicUri == null) stringResource(R.string.action_pick_sound)
                        else stringResource(R.string.action_change_sound),
                    )
                }
                if (settings.globalMusicUri != null) {
                    Text(
                        text = stringResource(R.string.label_sound_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.label_vacation_mode), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = vacationEnabled, onCheckedChange = { vacationEnabled = it })
                }
                
                if (vacationEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            val startText = vacationStart?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.label_start_date)
                            Text(startText, style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            val endText = vacationEnd?.let { dateFormatter.format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.label_end_date)
                            Text(endText, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    settings.copy(
                        defaultSnoozeMinutes = snoozeMinutes,
                        defaultMaxSnoozes = maxSnoozes,
                        gradualIncreaseSeconds = gradualIncreaseSeconds,
                        vacationModeEnabled = vacationEnabled,
                        vacationStartDate = vacationStart,
                        vacationEndDate = vacationEnd,
                    ),
                )
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = vacationStart)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vacationStart = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = vacationEnd)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vacationEnd = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                Text(
                    text = LocalTime.of(alarm.hour, alarm.minute).format(timeFormatter),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = getRepeatText(alarm.repeatDays),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
fun EditAlarmDialog(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onConfirm: (Alarm) -> Unit,
) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var repeatDays by remember { mutableStateOf(alarm.repeatDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_edit_alarm)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WheelPicker(
                            items = (0..23).toList(),
                            initialValue = hour,
                            onValueChange = { hour = it },
                            continuous = true,
                        )
                    }
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        WheelPicker(
                            items = (0..59).toList(),
                            initialValue = minute,
                            onValueChange = { minute = it },
                            continuous = true,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.label_repeat_on),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val dayNames = listOf(
                        stringResource(R.string.day_mon_short),
                        stringResource(R.string.day_tue_short),
                        stringResource(R.string.day_wed_short),
                        stringResource(R.string.day_thu_short),
                        stringResource(R.string.day_fri_short),
                        stringResource(R.string.day_sat_short),
                        stringResource(R.string.day_sun_short),
                    )
                    dayNames.forEachIndexed { index, day ->
                        val dayNum = index + 1
                        val isSelected = repeatDays.contains(dayNum)
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable {
                                    repeatDays = if (isSelected) {
                                        repeatDays - dayNum
                                    } else {
                                        repeatDays + dayNum
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(
                    alarm.copy(
                        hour = hour, 
                        minute = minute, 
                        repeatDays = repeatDays,
                    ),
                ) 
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun AlarmScreen(
    alarm: Alarm?,
    maxSnoozes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val canSnooze = alarm?.let { it.currentSnoozeCount < maxSnoozes } ?: true
    val snoozeCount = alarm?.currentSnoozeCount ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        // Snooze area (90%)
        Box(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .then(
                    if (canSnooze) Modifier.clickable { onSnooze() } else Modifier,
                )
                .background(if (canSnooze) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (canSnooze) stringResource(R.string.btn_snooze) else stringResource(R.string.label_no_more_snoozes),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    color = if (canSnooze) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                )
                if (canSnooze) {
                    Text(
                        text = stringResource(R.string.label_snooze_count, snoozeCount, maxSnoozes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = stringResource(R.string.label_tap_to_snooze),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // Dismiss area (10%)
        Box(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.btn_dismiss),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun getRepeatText(repeatDays: Set<Int>): String {
    return when {
        repeatDays.isEmpty() -> stringResource(R.string.repeat_once)
        repeatDays.size == 7 -> stringResource(R.string.repeat_everyday)
        repeatDays == setOf(1, 2, 3, 4, 5) -> stringResource(R.string.repeat_weekdays)
        repeatDays == setOf(6, 7) -> stringResource(R.string.repeat_weekends)
        else -> {
            val dayStrings = mutableListOf<String>()
            repeatDays.sorted().forEach { day ->
                dayStrings.add(stringResource(dayOfWeekRes(day)))
            }
            stringResource(R.string.repeat_prefix) + dayStrings.joinToString(", ")
        }
    }
}

private fun dayOfWeekRes(day: Int): Int {
    return when (day) {
        1 -> R.string.day_mon
        2 -> R.string.day_tue
        3 -> R.string.day_wed
        4 -> R.string.day_thu
        5 -> R.string.day_fri
        6 -> R.string.day_sat
        7 -> R.string.day_sun
        else -> 0
    }
}
