package com.ivan.englishalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("alarm_enabled", false)) {
                val hour = prefs.getInt("alarm_hour", 7)
                val minute = prefs.getInt("alarm_minute", 0)
                AlarmScheduler.scheduleNext(context, hour, minute)
            }
        }
    }
}
