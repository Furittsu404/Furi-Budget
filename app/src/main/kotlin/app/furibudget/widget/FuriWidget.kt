package app.furibudget.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.furibudget.BillNotifier
import app.furibudget.FuriBudgetApp
import app.furibudget.MainActivity
import app.furibudget.R
import app.furibudget.core.BillStatus
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.data.Bills
import app.furibudget.data.Ledger
import app.furibudget.data.TxKind
import app.furibudget.ui.theme.FuriColors
import app.furibudget.ui.theme.ThemePacks
import app.furibudget.ui.theme.ThemeRepository
import app.furibudget.ui.theme.resolveColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

/** One line in the widget's bill list. */
private data class WidgetBill(val name: String, val due: String, val amount: String, val status: BillStatus)

private data class WidgetData(val balance: String, val bills: List<WidgetBill>, val colors: FuriColors, val empty: String)

/**
 * Home-screen widget: total balance, the next bills, and a plus button that opens the Add screen. It follows the
 * user's theme pack and accent (colours only; the animated backgrounds stay in the app).
 */
class FuriWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { Content(context, data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val app = context.applicationContext as FuriBudgetApp
        val repo = app.repository
        val settings = app.settings.settings.first()
        val cur = Currency.byCode(settings.currencyCode)
        val accounts = repo.allAccounts.first()
        val balances = Ledger.balances(accounts, repo.transactions.first())
        val total = accounts.filter { !it.archived }.fold(Money.ZERO) { a, acc -> a + (balances[acc.id] ?: Money.ZERO) }
        val today = LocalDate.now()
        val soon = Bills.upcoming(repo.bills.first(), repo.billPayments.first(), today, 14).take(4)
        val bills = soon.map { occ ->
            val days = ChronoUnit.DAYS.between(today, occ.due).toInt()
            val due = when {
                occ.status == BillStatus.OVERDUE -> context.getString(R.string.overdue_days, -days)
                days == 0 -> context.getString(R.string.due_today)
                days == 1 -> context.getString(R.string.due_tomorrow)
                else -> context.getString(R.string.due_in_days, days)
            }
            val amount = (occ.payment?.amountMinor ?: occ.bill.amountMinor).let { if (it > 0) cur.format(Money(it), true) else context.getString(R.string.amount_varies) }
            WidgetBill(occ.bill.name, due, amount, occ.status)
        }
        val spec = ThemeRepository(context).spec.first()
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colors = resolveColors(spec, ThemePacks.byId(spec.packId), night, null)
        return WidgetData(cur.format(total, true), bills, colors, context.getString(R.string.nothing_due_week))
    }

    @Composable
    private fun Content(context: Context, d: WidgetData) {
        val c = d.colors
        fun col(color: Color) = ColorProvider(color)
        val open = actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val add = actionStartActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BillNotifier.EXTRA_ROUTE, "add_expense"),
        )
        val bills = actionStartActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BillNotifier.EXTRA_ROUTE, "bills"),
        )
        Box(GlanceModifier.fillMaxSize().background(col(c.background.copy(alpha = 0.94f))).cornerRadius(24.dp).clickable(open)) {
            Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(context.getString(R.string.total_balance), style = TextStyle(color = col(c.onSurfaceVariant), fontSize = 12.sp))
                        Text(d.balance, style = TextStyle(color = col(c.onSurface), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        GlanceModifier.size(44.dp).background(col(c.accent)).cornerRadius(22.dp).clickable(add),
                        contentAlignment = Alignment.Center,
                    ) { Text("+", style = TextStyle(color = col(c.onAccent), fontSize = 24.sp, fontWeight = FontWeight.Bold)) }
                }
                Spacer(GlanceModifier.size(8.dp))
                Column(GlanceModifier.fillMaxWidth().clickable(bills)) {
                    if (d.bills.isEmpty()) {
                        Text(d.empty, style = TextStyle(color = col(c.onSurfaceVariant), fontSize = 12.sp))
                    }
                    d.bills.forEach { b ->
                        Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(GlanceModifier.size(8.dp).background(col(statusTint(c, b.status))).cornerRadius(4.dp)) {}
                            Spacer(GlanceModifier.width(8.dp))
                            Text(b.name, style = TextStyle(color = col(c.onSurface), fontSize = 13.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                            Text(b.due, style = TextStyle(color = col(statusTint(c, b.status)), fontSize = 11.sp))
                            Spacer(GlanceModifier.width(8.dp))
                            Text(b.amount, style = TextStyle(color = col(c.onSurface), fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }

    private fun statusTint(c: FuriColors, s: BillStatus) = when (s) {
        BillStatus.OVERDUE -> c.expense
        BillStatus.DUE_TODAY, BillStatus.DUE_SOON -> c.warning
        BillStatus.PAID -> c.income
        else -> c.accentText
    }
}

class FuriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FuriWidget()
}

object WidgetUpdater {
    /** Redraws every placed widget. Cheap when none are placed. */
    suspend fun update(context: Context) {
        runCatching { FuriWidget().updateAll(context) }
    }
}
