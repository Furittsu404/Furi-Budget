package app.furibudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import app.furibudget.ui.LockGate
import app.furibudget.ui.theme.FuriBackground
import app.furibudget.ui.theme.FuriTheme
import app.furibudget.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val themeVm: ThemeViewModel by viewModels()
    private val budgetVm: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val spec = themeVm.spec.collectAsState().value ?: return@setContent
            val state = budgetVm.state.collectAsState().value
            FuriTheme(spec) {
                if (!state.loaded) {
                    FuriBackground()
                } else {
                    LockGate(enabled = state.settings.appLock) { AppRoot(spec, themeVm::update, budgetVm) }
                }
            }
        }
    }
}
