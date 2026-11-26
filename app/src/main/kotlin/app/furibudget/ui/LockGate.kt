package app.furibudget.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.furibudget.R
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriBackground

private const val GRACE_MS = 30_000L

/**
 * Hides [content] behind the phone's own screen lock. Locks on a cold start and after the app has been
 * away for 30 seconds. While enabled, screenshots and the recents preview are blocked.
 */
@Composable
fun LockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val keyguard = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    var locked by rememberSaveable { mutableStateOf(true) }
    var stoppedAt by remember { mutableStateOf(0L) }
    val secure = keyguard.isDeviceSecure
    val active = enabled && secure

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, active) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) stoppedAt = SystemClock.elapsedRealtime()
            if (e == Lifecycle.Event.ON_START && active && stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > GRACE_MS) locked = true
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    SideEffect {
        val w = (context as Activity).window
        if (active) w.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r -> if (r.resultCode == Activity.RESULT_OK) locked = false }
    fun prompt() {
        val intent = keyguard.createConfirmDeviceCredentialIntent(context.getString(R.string.app_name), context.getString(R.string.unlock_prompt))
        if (intent == null) locked = false else ask.launch(intent)
    }
    LaunchedEffect(locked, active) { if (active && locked) prompt() }
    LaunchedEffect(active) { if (!active) locked = false }

    if (active && locked) {
        Box(Modifier.fillMaxSize()) {
            FuriBackground()
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.Lock, null, tint = Furi.colors.accentText)
                Text(stringResource(R.string.locked), style = MaterialTheme.typography.headlineSmall, color = Furi.colors.onSurface)
                Button(onClick = ::prompt) { Text(stringResource(R.string.unlock)) }
            }
        }
    } else content()
}
