package app.furibudget

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.furibudget.core.BillSchedule
import app.furibudget.core.BillStatus
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.data.AppSettings
import app.furibudget.data.BillEntity
import app.furibudget.data.BillOccurrence
import app.furibudget.data.Bills
import app.furibudget.data.TxKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where a notification or launcher shortcut wants the app to land. */
data class LaunchRequest(val route: String, val day: Long? = null, val id: Long? = null)

/**
 * Bill reminders. One daily alarm runs [check]: it pays auto-pay bills that came due, then tells the user about
 * bills that are almost due, due today, or overdue, once per stage (see BillAlertEntity).
 */
object BillNotifier {
    const val CHANNEL = "bills"
    const val EXTRA_ROUTE = "route"
    const val EXTRA_DAY = "day"
    const val ACTION_PAID = "app.furibudget.PAID"
    private const val STAGE_EARLY = 0
    private const val STAGE_TODAY = 1
    private const val STAGE_OVERDUE = 2
    private const val STAGE_OVERDUE_WEEK = 3
    private const val GROUP_LIMIT = 3

    private fun canPost(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openIntent(context: Context, day: Long?): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ROUTE, "bills").apply { if (day != null) putExtra(EXTRA_DAY, day) }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, (day ?: 0L).toInt(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun notificationId(b: BillEntity, due: LocalDate) = (b.id * 100_000 + due.toEpochDay() % 100_000).toInt()

    @android.annotation.SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, title: String, text: String, open: PendingIntent, paid: PendingIntent? = null) {
        if (!canPost(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true)
        if (paid != null) b.addAction(0, context.getString(R.string.mark_paid), paid)
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    private fun paidIntent(context: Context, b: BillEntity, due: LocalDate): PendingIntent {
        val i = Intent(context, PayActionReceiver::class.java).setAction(ACTION_PAID)
            .putExtra("billId", b.id).putExtra(EXTRA_DAY, due.toEpochDay())
        return PendingIntent.getBroadcast(context, notificationId(b, due), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Which stage, if any, this occurrence is in today. Null means nothing to say. */
    fun stageFor(occ: BillOccurrence, today: LocalDate): Int? {
        val remind = occ.bill.remindDaysBefore
        if (remind < 0 || !occ.isOpen) return null
        val days = ChronoUnit.DAYS.between(today, occ.due)
        return when {
            days > 0 && days <= remind -> STAGE_EARLY
            days == 0L -> STAGE_TODAY
            days <= -7 -> STAGE_OVERDUE_WEEK
            days < 0 -> STAGE_OVERDUE
            else -> null
        }
    }

    /** Runs the daily pass. Returns how many notifications were shown (for the "send now" button). */
    suspend fun check(context: Context, today: LocalDate = LocalDate.now()): Int {
        val app = context.applicationContext as FuriBudgetApp
        val repo = app.repository
        val settings = app.settings.settings.first()
        val cur = Currency.byCode(settings.currencyCode)
        var shown = 0

        // 1. Auto-pay bills that came due: tell the user what was recorded.
        val paid = repo.postAutoPay(today)
        if (settings.billReminders) {
            for ((bill, due) in paid.takeLast(GROUP_LIMIT)) {
                post(
                    context, notificationId(bill, due) + 7, context.getString(R.string.autopaid_title, bill.name),
                    context.getString(R.string.autopaid_text, cur.format(Money(bill.amountMinor), true)), openIntent(context, due.toEpochDay()),
                )
                shown++
            }
        }
        if (!settings.billReminders) return shown

        // 2. Reminders for what is still open.
        val bills = repo.activeBills()
        val payments = repo.allPayments()
        val open = Bills.occurrences(bills, payments, today.minusDays(BillSchedule.LOOKBACK_DAYS), today.plusDays(30), today)
            .filter { !it.bill.autoPay }
        val due = ArrayList<Pair<BillOccurrence, Int>>()
        for (occ in open) {
            val stage = stageFor(occ, today) ?: continue
            if (repo.claimAlert(occ.bill.id, occ.due, stage)) due += occ to stage
        }
        repo.pruneAlerts(today)
        if (due.isEmpty()) return shown

        if (due.size <= GROUP_LIMIT) {
            for ((occ, stage) in due) {
                val b = occ.bill
                val days = ChronoUnit.DAYS.between(today, occ.due).toInt()
                val title = when (stage) {
                    STAGE_EARLY -> if (b.kind == TxKind.INCOME) context.getString(R.string.notif_income_in, b.name, days) else context.getString(R.string.notif_due_in, b.name, days)
                    STAGE_TODAY -> context.getString(if (b.kind == TxKind.INCOME) R.string.notif_income_today else R.string.notif_due_today, b.name)
                    else -> context.getString(R.string.notif_overdue, b.name, -days)
                }
                val text = if (b.amountMinor > 0) cur.format(Money(b.amountMinor), true) else context.getString(R.string.amount_varies)
                post(context, notificationId(b, occ.due), title, text, openIntent(context, occ.due.toEpochDay()), if (b.amountMinor > 0) paidIntent(context, b, occ.due) else null)
                shown++
            }
        } else {
            val total = due.sumOf { it.first.bill.amountMinor }
            post(
                context, 90_001, context.getString(R.string.notif_summary_title, due.size),
                context.getString(R.string.notif_summary_text, cur.format(Money(total), true), due.take(3).joinToString { it.first.bill.name }),
                openIntent(context, today.toEpochDay()),
            )
            shown++
        }
        return shown
    }

    private fun alarmIntent(context: Context) =
        PendingIntent.getBroadcast(context, 3, Intent(context, BillAlertReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Arms the next daily check, or cancels it when reminders are off. Inexact, so no exact-alarm permission is needed. */
    fun schedule(context: Context, s: AppSettings) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (!s.billReminders) { am.cancel(alarmIntent(context)); return }
        val now = LocalDateTime.now()
        var at = now.withHour(s.billHour).withMinute(s.billMinute).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), alarmIntent(context))
    }
}

private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch { try { block() } finally { pending.finish() } }
}

class BillAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        BillNotifier.check(context)
        val s = (context.applicationContext as FuriBudgetApp).settings.settings.first()
        BillNotifier.schedule(context, s)
    }
}

/** The "Paid" button on a bill notification: records the expense at the bill's usual account and amount. */
class PayActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async {
        val app = context.applicationContext as FuriBudgetApp
        val billId = intent.getLongExtra("billId", 0L)
        val due = LocalDate.ofEpochDay(intent.getLongExtra(BillNotifier.EXTRA_DAY, 0L))
        val bill = app.repository.bill(billId) ?: return@async
        app.repository.payBill(bill, due, bill.amountMinor, bill.accountId, LocalDate.now())
        NotificationManagerCompat.from(context).cancel((bill.id * 100_000 + due.toEpochDay() % 100_000).toInt())
    }
}
