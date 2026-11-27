package app.furibudget.ui.home

import app.furibudget.ui.theme.entityColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.funds.FreeBar
import app.furibudget.ui.funds.accountMoney
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

/**
 * One row per open account: what it holds, what is set aside in it and what is free to spend, with the same
 * three-part bar as the total above. Only shown when there is more than one account to tell apart.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(state: BudgetState, modifier: Modifier = Modifier) {
    if (state.accounts.size < 2) return
    val c = Furi.colors
    val cur = state.currency
    val rows = remember(state.accounts, state.balances, state.funds, state.fundMoves, state.transactions, state.bills, state.billPayments) { accountMoney(state) }
    FuriCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.accounts_title), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
            rows.forEach { r ->
                val tint = entityColor(r.account.colorArgb)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            FuriIcon(r.account.icon, tint = tint, size = 22.dp)
                        }
                        Text(r.account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(cur.format(Money(r.balanceMinor), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.onSurface)
                    }
                    if (r.reservedMinor > 0 || r.billsDueMinor > 0) {
                        FreeBar(r.freeMinor, r.billsDueMinor, r.reservedMinor)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(stringResource(R.string.acct_free, cur.format(Money(r.freeMinor), true)), style = MaterialTheme.typography.labelMedium, color = if (r.freeMinor < 0) c.expense else c.accentText)
                            if (r.reservedMinor > 0) Text(stringResource(R.string.acct_reserved, cur.format(Money(r.reservedMinor), true)), style = MaterialTheme.typography.labelMedium, color = c.income)
                            if (r.billsDueMinor > 0) Text(stringResource(R.string.acct_bills_due, cur.format(Money(r.billsDueMinor), true)), style = MaterialTheme.typography.labelMedium, color = c.warning)
                        }
                    } else {
                        Text(stringResource(R.string.acct_free, cur.format(Money(r.freeMinor), true)), style = MaterialTheme.typography.labelMedium, color = if (r.freeMinor < 0) c.expense else c.accentText)
                    }
                }
            }
        }
    }
}
