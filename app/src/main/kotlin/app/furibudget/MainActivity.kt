package app.furibudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.furibudget.ui.HomeStub
import app.furibudget.ui.appearance.AppearanceScreen
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriBackground
import app.furibudget.ui.theme.FuriTheme
import app.furibudget.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val themeVm: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val spec = themeVm.spec.collectAsState().value ?: return@setContent
            FuriTheme(spec) {
                var appearance by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = appearance) { appearance = false }
                val m = Furi.motion
                Box(Modifier.fillMaxSize()) {
                    FuriBackground()
                    AnimatedContent(
                        targetState = appearance,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally(m.spring()) { it / 4 } + fadeIn(m.tween(200))) togetherWith
                                    (slideOutHorizontally(m.spring()) { -it / 6 } + fadeOut(m.tween(160)))
                            } else {
                                (slideInHorizontally(m.spring()) { -it / 6 } + fadeIn(m.tween(200))) togetherWith
                                    (slideOutHorizontally(m.spring()) { it / 4 } + fadeOut(m.tween(160)))
                            }.using(SizeTransform(clip = false))
                        },
                        label = "screen",
                    ) { showAppearance ->
                        if (showAppearance) AppearanceScreen(spec, themeVm::update) { appearance = false }
                        else HomeStub { appearance = true }
                    }
                }
            }
        }
    }
}
