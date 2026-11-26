package app.furibudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.furibudget.ui.theme.FuriTheme
import app.furibudget.ui.HomePlaceholder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FuriTheme {
                HomePlaceholder()
            }
        }
    }
}
