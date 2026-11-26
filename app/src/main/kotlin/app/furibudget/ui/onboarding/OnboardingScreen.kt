package app.furibudget.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import app.furibudget.data.AccountEntity
import app.furibudget.data.Budgeting
import app.furibudget.ui.appearance.PackTile
import app.furibudget.ui.appearance.Swatch
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.theme.AccentChoice
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.ThemePacks
import app.furibudget.ui.theme.ThemeSpec
import kotlinx.coroutines.launch

/** First-run flow: what the app is, pick a look, say what you have now. Theme changes show live behind it. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    state: BudgetState,
    spec: ThemeSpec,
    onTheme: ((ThemeSpec) -> ThemeSpec) -> Unit,
    onSaveAccount: (AccountEntity) -> Unit,
    onDone: () -> Unit,
) {
    val c = Furi.colors
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val balances = remember { mutableStateMapOf<Long, String>() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage < 2) TextButton(onClick = onDone) { Text(stringResource(R.string.skip)) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                when (page) {
                    0 -> {
                        var fill by remember { mutableStateOf(0f) }
                        androidx.compose.runtime.LaunchedEffect(pager.currentPage) { fill = if (pager.currentPage == 0) 0.68f else 0f }
                        BudgetRing(fill, Budgeting.Level.OK, Modifier.size(220.dp), stroke = 18.dp) {
                            Text("68%", style = Furi.figures.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize), color = c.onSurface)
                        }
                        Text(stringResource(R.string.onb_title_1), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.onb_body_1), style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    1 -> {
                        Text(stringResource(R.string.onb_title_2), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, textAlign = TextAlign.Center)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ThemePacks.all.forEach { pack -> PackTile(pack, spec, spec.packId == pack.id) { onTheme { it.copy(packId = pack.id) } } }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Accents.swatches.take(12).forEach { sw ->
                                Swatch(Color(sw.argb), sw.name, (spec.accent as? AccentChoice.Swatch)?.id == sw.id) { onTheme { it.copy(accent = AccentChoice.Swatch(sw.id)) } }
                            }
                        }
                    }
                    else -> {
                        Text(stringResource(R.string.onb_title_3), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.onb_body_3), style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                        state.accounts.forEach { a ->
                            AmountField(balances[a.id] ?: "", { balances[a.id] = it }, state.currency, a.name, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val w by animateDpAsState(if (pager.currentPage == i) 28.dp else 8.dp, Furi.motion.spring(), label = "dot")
                    Box(Modifier.height(8.dp).size(w, 8.dp).clip(CircleShape).background(if (pager.currentPage == i) c.accent else c.onSurface.copy(alpha = 0.25f)))
                }
            }
            if (pager.currentPage < 2) {
                Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text(stringResource(R.string.next)) }
            } else {
                Button(onClick = {
                    state.accounts.forEach { a ->
                        val typed = state.currency.parse(balances[a.id].orEmpty().ifBlank { "0" })
                        if (typed != null && typed != Money(a.startBalance)) onSaveAccount(a.copy(startBalance = typed.minor))
                    }
                    onDone()
                }) { Text(stringResource(R.string.get_started)) }
            }
        }
    }
}
