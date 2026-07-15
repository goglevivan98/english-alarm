package com.ivan.englishalarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "alarm_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Будильник с заданием",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val quizIntent = Intent(context, QuizActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, quizIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Пора отвечать!")
            .setContentText("Переведи слово, чтобы выключить будильник")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        nm.notify(1, notification)

        // Пытаемся также запустить экран напрямую (работает, если приложение
        // сейчас не в фоновых ограничениях; full screen intent выше — основной путь).
        context.startActivity(quizIntent)

        // Планируем будильник на завтра то же время, если он ещё включён.
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("alarm_enabled", false)) {
            val hour = prefs.getInt("alarm_hour", 7)
            val minute = prefs.getInt("alarm_minute", 0)
            AlarmScheduler.scheduleNext(context, hour, minute)
        }
    }
}
