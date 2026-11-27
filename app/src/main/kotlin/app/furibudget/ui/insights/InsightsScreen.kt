package app.furibudget.ui.insights

import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.core.Periods
import app.furibudget.data.Insights
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.BarGroup
import app.furibudget.ui.components.DonutChart
import app.furibudget.ui.components.PairedBars
import app.furibudget.ui.components.Slice
import app.furibudget.ui.components.TrendLine
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
private val dayFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val monthOnly = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

@Composable
fun InsightsScreen(state: BudgetState) {
    val c = Furi.colors
    val cur = state.currency
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val range = remember(offset, state.startDay) { Periods.shiftMonth(state.month(today), offset, state.startDay) }
    val prevRange = remember(range) { Periods.shiftMonth(range, -1, state.startDay) }

    val cats = remember(state.transactions, state.splits, range) { Insights.spendByCategory(state.transactions, range, state.splits) }
    val fallback = c.onSurfaceVariant
    val slices = cats.map { ct ->
        val cat = state.category(ct.categoryId)
        Slice(cat?.name ?: stringResource(R.string.uncategorised), ct.amount.minor, cat?.let { Color(it.colorArgb) } ?: fallback)
    }
    val spent = Money(cats.sumOf { it.amount.minor })
    var picked by remember(range) { mutableStateOf<Int?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 168.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.tab_insights), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { offset -= 1 }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.prev_month)) }
            Text(range.start.format(monthFmt), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
            IconButton(onClick = { offset += 1 }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month)) }
        }

        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.where_it_went), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                if (slices.isEmpty()) {
                    Text(stringResource(R.string.no_spending), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(slices, picked, { picked = it }, Modifier.fillMaxWidth(0.78f), description = stringResource(R.string.chart_donut, slices.joinToString { "${it.label} ${(it.value * 100f / spent.minor.coerceAtLeast(1)).roundToInt()}%" })) {
                            val sel = picked?.let { slices.getOrNull(it) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
                                Text(sel?.label ?: stringResource(R.string.spent), style = MaterialTheme.typography.labelLarge, color = sel?.color ?: c.onSurfaceVariant, textAlign = TextAlign.Center)
                                AnimatedAmount(
                                    cur.format(Money(sel?.value ?: spent.minor)),
                                    Furi.figures.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), c.onSurface, rollFromZero = false,
                                )
                                if (sel != null) Text("${(sel.value * 100f / spent.minor.coerceAtLeast(1)).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                            }
                        }
                    }
                    slices.forEachIndexed { i, s ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(MaterialTheme.shapes.small).clickable { picked = if (picked == i) null else i },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(s.color))
                            Text(s.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                            Text("${(s.value * 100f / spent.minor.coerceAtLeast(1)).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                            Text(cur.format(Money(s.value)), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.onSurface)
                        }
                    }
                }
            }
        }

        val byTag = remember(state.transactions, state.transactionTags, range) { Insights.spendByTag(state.transactions, range, state.transactionTags) }
        if (byTag.isNotEmpty()) {
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.by_tag), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    val top = byTag.first().second.minor.coerceAtLeast(1)
                    byTag.take(8).forEach { (id, amt) ->
                        val name = state.tags.firstOrNull { it.id == id }?.name ?: return@forEach
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                                Text(cur.format(amt), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.onSurface)
                            }
                            val frac by androidx.compose.animation.core.animateFloatAsState(amt.minor.toFloat() / top, Furi.motion.spring(), label = "tag-bar")
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.onSurface.copy(alpha = 0.1f))) {
                                Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(c.accent))
                            }
                        }
                    }
                }
            }
        }

        val bars = remember(state.transactions, range) { Insights.monthBars(state.transactions, range.start, 6, state.startDay) }
        var barSel by remember(range) { mutableIntStateOf(bars.lastIndex) }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.last_months), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                if (bars.all { it.income.minor == 0L && it.expense.minor == 0L }) {
                    app.furibudget.ui.components.EmptyState(androidx.compose.material.icons.Icons.Rounded.BarChart, stringResource(R.string.insights_empty_title), stringResource(R.string.nothing_recorded))
                } else {
                    PairedBars(bars.map { BarGroup(it.range.start.format(monthShort), it.income.minor, it.expense.minor) }, c.income, c.expense, barSel, { barSel = it }, description = stringResource(R.string.chart_bars, bars[barSel].let { "${it.range.start.format(monthShort)}: +${cur.format(it.income)}, -${cur.format(it.expense)}" }))
                }
                Row(Modifier.fillMaxWidth()) {
                    bars.forEachIndexed { i, b ->
                        Text(
                            b.range.start.format(monthShort), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, color = if (i == barSel) c.onSurface else c.onSurfaceVariant,
                        )
                    }
                }
                val sel = bars[barSel]
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("+ " + cur.format(sel.income), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.income)
                    Text("− " + cur.format(sel.expense), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.expense)
                }
            }
        }

        val inRange = today in range
        val cum = remember(state.transactions, range) { Insights.cumulativeSpend(state.transactions, range, if (inRange) today else null) }
        val prevCum = remember(state.transactions, prevRange) { Insights.cumulativeSpend(state.transactions, prevRange) }
        var scrub by remember(range) { mutableStateOf<Int?>(null) }
        val shown = scrub ?: if (inRange) (today.toEpochDay() - range.start.toEpochDay()).toInt().coerceIn(0, cum.lastIndex) else cum.lastIndex
        val vsIdx = shown.coerceAtMost(prevCum.lastIndex)
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.spending_pace), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(cur.format(Money(cum[shown]), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = c.onSurface)
                    Text(range.start.plusDays(shown.toLong()).format(dayFmt), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
                TrendLine(cum, prevCum, scrub, { scrub = it }, c.accentBrush, c.accent, description = stringResource(R.string.chart_trend, cur.format(Money(cum[shown]), true), range.start.plusDays(shown.toLong()).format(dayFmt)))
                val before = prevCum.getOrNull(vsIdx) ?: 0L
                val now = cum[shown]
                val text = when {
                    before == 0L && now == 0L -> stringResource(R.string.no_spending)
                    before == 0L -> stringResource(R.string.nothing_before, prevRange.start.format(monthOnly))
                    else -> {
                        val pct = ((now - before) * 100f / before).roundToInt()
                        val name = prevRange.start.format(monthOnly)
                        if (pct >= 0) stringResource(R.string.more_than, abs(pct), name) else stringResource(R.string.less_than, abs(pct), name)
                    }
                }
                Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
        }
    }
}
