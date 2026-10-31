package com.dotilda.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Notification
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Exact alarms need no permission before Android 12; from 12 on the user may have refused them.
 * (canScheduleExactAlarms doesn't exist on Android 11: calling it there crashed at launch.)
 */
fun AlarmManager.mayRingExactly(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()

/** The one daily appointment. A single exact alarm, re-armed after it fires and after a reboot. */
object Reminder {
    private const val CHANNEL = "daily_question"
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, on: Boolean, minutes: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context)
        alarms.cancel(intent)
        if (!on) return

        val now = LocalDateTime.now()
        var next = now.toLocalDate().atStartOfDay().plusMinutes(minutes.toLong())
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (alarms.mayRingExactly()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun notify(context: Context) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Fires the reminder (unless today is already written) and re-arms it; also re-arms on boot. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DotildaApp
        val pending = goAsync()
        MainScope().launch {
            try {
                val settings = app.prefs.settingsNow()
                if (intent.action == null && settings.reminderOn && !app.journal.hasAnswered(LocalDate.now())) {
                    Reminder.notify(context)
                }
                Reminder.schedule(context, settings.reminderOn, settings.reminderMinutes)
            } finally {
                pending.finish()
            }
        }
    }
}
