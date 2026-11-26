package app.furibudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import app.furibudget.ui.LockGate
import app.furibudget.ui.theme.FuriBackground
import app.furibudget.ui.theme.FuriTheme
import app.furibudget.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val themeVm: ThemeViewModel by viewModels()
    private val budgetVm: BudgetViewModel by viewModels()
    private val launch = mutableStateOf<LaunchRequest?>(null)

    private fun readLaunch(intent: android.content.Intent?) {
        val route = intent?.getStringExtra(BillNotifier.EXTRA_ROUTE) ?: return
        launch.value = LaunchRequest(
            route,
            intent.takeIf { it.hasExtra(BillNotifier.EXTRA_DAY) }?.getLongExtra(BillNotifier.EXTRA_DAY, 0L),
            intent.takeIf { it.hasExtra(Shortcuts.EXTRA_TEMPLATE) }?.getLongExtra(Shortcuts.EXTRA_TEMPLATE, 0L),
        )
        intent.removeExtra(BillNotifier.EXTRA_ROUTE)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        readLaunch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readLaunch(intent)
        setContent {
            val spec = themeVm.spec.collectAsState().value ?: return@setContent
            val state = budgetVm.state.collectAsState().value
            FuriTheme(spec) {
                if (!state.loaded) {
                    FuriBackground()
                } else {
                    LockGate(enabled = state.settings.appLock) { AppRoot(spec, themeVm::update, budgetVm, launch.value) { launch.value = null } }
                }
            }
        }
    }
}
