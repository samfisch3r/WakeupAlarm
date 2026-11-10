package com.wakeupalarm.android.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeupalarm.android.R
import com.wakeupalarm.android.data.Alarm
import com.wakeupalarm.android.data.Settings
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    settings: Settings,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onPickMusic: ((Uri) -> Unit) -> Unit,
    onUpdateSettings: (Settings) -> Unit,
) {
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val nextAlarmInfo = remember(alarms, settings) {
        calculateNextAlarmInfo(alarms, settings)
    }

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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            nextAlarmInfo?.let { info ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Next Scheduled Alarm",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = info.displayString,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Alarms Set",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + below to create a new alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
    onPickMusic: ((Uri) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var snoozeMinutes by remember { mutableIntStateOf(settings.defaultSnoozeMinutes) }
    var maxSnoozes by remember { mutableIntStateOf(settings.defaultMaxSnoozes) }
    var gradualIncreaseSeconds by remember { mutableIntStateOf(settings.gradualIncreaseSeconds) }
    var globalVolume by remember { mutableFloatStateOf(settings.globalVolume.toFloat()) }
    var globalVibrate by remember { mutableStateOf(settings.globalVibrate) }
    var vacationEnabled by remember { mutableStateOf(settings.vacationModeEnabled) }
    var vacationStart by remember { mutableStateOf(settings.vacationStartDate) }
    var vacationEnd by remember { mutableStateOf(settings.vacationEndDate) }
    var customMusicUri by remember { mutableStateOf(settings.globalMusicUri) }

    LaunchedEffect(settings.globalMusicUri) {
        customMusicUri = settings.globalMusicUri
    }

    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val fileName = remember(customMusicUri) {
        getFileNameFromUri(context, customMusicUri)
    }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    var activePicker by remember { mutableStateOf<String?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            onDismiss()
        },
        title = { Text(stringResource(R.string.dialog_global_settings)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Alarm Volume", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${globalVolume.roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = globalVolume,
                    onValueChange = { globalVolume = it },
                    valueRange = 0f..100f,
                    steps = 100,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vibration toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Vibrate on Alarm", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = globalVibrate, onCheckedChange = { globalVibrate = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

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
                        textAlign = TextAlign.End
                    )
                }
                if (activePicker == "snooze") {
                    WheelPicker(
                        items = (1..60).toList(),
                        initialValue = snoozeMinutes,
                        onValueChange = { snoozeMinutes = it },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePicker = if (activePicker == "max") null else "max" }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.label_max_snoozes), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = maxSnoozes.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                if (activePicker == "max") {
                    WheelPicker(
                        items = (1..20).toList(),
                        initialValue = maxSnoozes,
                        onValueChange = { maxSnoozes = it },
                    )
                }

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
                        textAlign = TextAlign.End
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
                    onClick = {
                        previewPlayer?.stop()
                        previewPlayer?.release()
                        previewPlayer = null
                        isPreviewPlaying = false
                        onPickMusic { uri ->
                            customMusicUri = uri.toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (customMusicUri == null) stringResource(R.string.action_pick_sound)
                        else stringResource(R.string.action_change_sound),
                    )
                }

                if (customMusicUri != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Selected: ${fileName ?: "Custom Audio"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isPreviewPlaying) {
                                            previewPlayer?.stop()
                                            previewPlayer?.release()
                                            previewPlayer = null
                                            isPreviewPlaying = false
                                        } else {
                                            try {
                                                previewPlayer?.release()
                                                previewPlayer = MediaPlayer().apply {
                                                    setAudioAttributes(
                                                        AudioAttributes.Builder()
                                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                            .build()
                                                    )
                                                    setDataSource(context, Uri.parse(customMusicUri))
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        isPreviewPlaying = false
                                                    }
                                                }
                                                isPreviewPlaying = true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (isPreviewPlaying) "Stop Preview" else "Preview Sound")
                                }

                                TextButton(onClick = {
                                    previewPlayer?.stop()
                                    previewPlayer?.release()
                                    previewPlayer = null
                                    isPreviewPlaying = false
                                    customMusicUri = null
                                }) {
                                    Text("Reset Sound", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
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
                            val startText = vacationStart?.let { DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.label_start_date)
                            Text(startText, style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            val endText = vacationEnd?.let { DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it)) } ?: stringResource(R.string.label_end_date)
                            Text(endText, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
                onConfirm(
                    settings.copy(
                        defaultSnoozeMinutes = snoozeMinutes,
                        defaultMaxSnoozes = maxSnoozes,
                        gradualIncreaseSeconds = gradualIncreaseSeconds,
                        globalVolume = globalVolume.roundToInt(),
                        globalVibrate = globalVibrate,
                        vacationModeEnabled = vacationEnabled,
                        vacationStartDate = vacationStart,
                        vacationEndDate = vacationEnd,
                        globalMusicUri = customMusicUri
                    ),
                )
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
                onDismiss()
            }) { Text(stringResource(R.string.action_cancel)) }
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
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        repeatDays = repeatDays
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
    nextAlarmText: String? = null,
    maxSnoozes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val canSnooze = alarm?.let { it.currentSnoozeCount < maxSnoozes } ?: true
    val snoozeCount = alarm?.currentSnoozeCount ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
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

                if (!nextAlarmText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Next alarm: $nextAlarmText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

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

fun calculateNextAlarmInfo(alarms: List<Alarm>, settings: Settings): NextAlarmInfo? {
    val enabledAlarms = alarms.filter { it.isEnabled }
    if (enabledAlarms.isEmpty()) return null

    val now = LocalDateTime.now()

    if (settings.vacationModeEnabled) {
        val nowMs = Instant.now().toEpochMilli()
        val start = settings.vacationStartDate ?: 0L
        val end = (settings.vacationEndDate ?: 0L) + 86_399_999L
        if (nowMs in start..end) {
            val nonRepeating = enabledAlarms.filter { it.repeatDays.isEmpty() }
            if (nonRepeating.isEmpty()) return null
        }
    }

    val upcomingTimes = enabledAlarms.map { alarm ->
        var target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(alarm.hour, alarm.minute))
        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusDays(1)
        }
        if (alarm.repeatDays.isNotEmpty()) {
            while (!alarm.repeatDays.contains(target.dayOfWeek.value)) {
                target = target.plusDays(1)
            }
        }
        target
    }

    val nextTarget = upcomingTimes.minOrNull() ?: return null

    val duration = Duration.between(now, nextTarget)
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60

    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    val timeFormatted = nextTarget.format(timeFormat)

    val dayText = when {
        nextTarget.toLocalDate() == now.toLocalDate() -> "Today"
        nextTarget.toLocalDate() == now.toLocalDate().plusDays(1) -> "Tomorrow"
        else -> nextTarget.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    val remainingText = when {
        hours > 0 && mins > 0 -> "$hours hr $mins min"
        hours > 0 -> "$hours hr"
        mins > 0 -> "$mins min"
        else -> "< 1 min"
    }

    return NextAlarmInfo(
        displayString = "$dayText at $timeFormatted (in $remainingText)",
        targetDateTime = nextTarget
    )
}

fun getFileNameFromUri(context: Context, uriString: String?): String? {
    if (uriString.isNullOrEmpty()) return null
    val uri = uriString.toUri()
    var fileName: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (fileName == null) {
        fileName = uri.path?.substringAfterLast('/')
    }
    return fileName
}

data class NextAlarmInfo(
    val displayString: String,
    val targetDateTime: LocalDateTime
)
