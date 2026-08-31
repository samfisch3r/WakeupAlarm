package com.wakeupalarm.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakeupalarm.android.data.AlarmDatabase
import com.wakeupalarm.android.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AlarmScheduler(context)
            val db = AlarmDatabase.getInstance(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val alarms = db.alarmDao().getAllAlarms().first()
                alarms.forEach { alarm ->
                    if (alarm.isEnabled) {
                        scheduler.schedule(alarm)
                    }
                }
            }
        }
    }
}
