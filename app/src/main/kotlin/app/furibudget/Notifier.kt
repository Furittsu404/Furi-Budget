package app.furibudget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

object Notifier {
    private const val CH_REMINDER = "reminder"
    private const val CH_ALERT = "alert"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, context.getString(R.string.channel_reminder), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, context.getString(R.string.channel_alert), NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun canPost(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // canPost() checks POST_NOTIFICATIONS right above; lint cannot see through the helper.
    @android.annotation.SuppressLint("MissingPermission")
    private fun post(context: Context, channel: String, id: Int, title: String, text: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    fun dailyReminder(context: Context) =
        post(context, CH_REMINDER, 1, context.getString(R.string.reminder_title), context.getString(R.string.reminder_text))

    fun budgetAlert(context: Context, name: String, percent: Int) =
        post(context, CH_ALERT, 100 + name.hashCode().and(0xFFF), context.getString(R.string.alert_title, name), context.getString(R.string.alert_text, percent))

    private fun alarmIntent(context: Context, receiver: Class<*>, code: Int) =
        PendingIntent.getBroadcast(context, code, Intent(context, receiver), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Next [hour]:[minute] local time, in epoch millis. */
    private fun nextAt(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var at = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Inexact on purpose: no exact-alarm permission needed, and a few minutes of drift is fine for a reminder. */
    fun scheduleReminder(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(hour, minute), alarmIntent(context, ReminderReceiver::class.java, 1))
    }

    fun cancelReminder(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, ReminderReceiver::class.java, 1))
    }

    /** Twice a day, post any recurring transactions that have come due. The app also does this every time it opens. */
    fun scheduleRecurring(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HALF_DAY, alarmIntent(context, RecurringReceiver::class.java, 2))
    }
}

/** Runs a suspend block for a broadcast without the process being killed half way. */
private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try { block() } finally { pending.finish() }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        val app = context.applicationContext as FuriBudgetApp
        val s = app.settings.settings.first()
        if (!s.dailyReminder) return@async
        app.repository.postDueRecurrences(java.time.LocalDate.now())
        Notifier.dailyReminder(context)
        Notifier.scheduleReminder(context, s.reminderHour, s.reminderMinute)
    }
}

class RecurringReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        (context.applicationContext as FuriBudgetApp).repository.postDueRecurrences(java.time.LocalDate.now())
    }
}

/** Alarms are lost on reboot, so put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        val s = (context.applicationContext as FuriBudgetApp).settings.settings.first()
        Notifier.scheduleRecurring(context)
        if (s.dailyReminder) Notifier.scheduleReminder(context, s.reminderHour, s.reminderMinute)
    }
}
