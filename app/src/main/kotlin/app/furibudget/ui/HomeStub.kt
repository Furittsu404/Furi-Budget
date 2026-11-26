package app.furibudget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.R
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

/** Stand-in until M3 builds the real Home. Exists so the theme has something to dress. */
@Composable
fun HomeStub(onOpenAppearance: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge, color = Furi.colors.onSurface)
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_sample_left), style = MaterialTheme.typography.bodyMedium, color = Furi.colors.onSurfaceVariant)
                Text("₱ 18,420.50", style = Furi.figures, color = Furi.colors.onSurface)
            }
        }
        FilledTonalButton(onClick = onOpenAppearance) { Text(stringResource(R.string.open_appearance)) }
    }
}
