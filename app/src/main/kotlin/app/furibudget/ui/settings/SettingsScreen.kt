package app.furibudget.ui.settings

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.BudgetViewModel
import app.furibudget.R
import app.furibudget.core.Currency
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import app.furibudget.ui.theme.ThemeSpec
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: BudgetState,
    vm: BudgetViewModel,
    spec: ThemeSpec,
    onTheme: ((ThemeSpec) -> ThemeSpec) -> Unit,
    onOpenManage: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenTrash: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Furi.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = state.settings
    val msg_backup_invalid = stringResource(R.string.backup_invalid)
    val msg_backup_restored = stringResource(R.string.backup_restored)
    val msg_backup_saved = stringResource(R.string.backup_saved)
    val msg_csv_saved = stringResource(R.string.csv_saved)
    val msg_sample_loaded = stringResource(R.string.sample_loaded)
    var confirmImport by remember { mutableStateOf<String?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var pickBillTime by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotificationsIfNeeded() { if (needsPermission) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val text = vm.exportJson(spec)
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            message = msg_backup_saved
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val text = vm.exportCsv()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            message = msg_csv_saved
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }
            if (text == null || app.furibudget.data.Backup.decode(text) == null) message = msg_backup_invalid else confirmImport = text
        }
    }
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }

        Group(stringResource(R.string.general)) {
            Text(stringResource(R.string.currency), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Currency.all.forEach { cur ->
                    FilterChip(selected = s.currencyCode == cur.code, onClick = { vm.updateSettings { it.copy(currencyCode = cur.code) } }, label = { Text("${cur.symbol} ${cur.code}") })
                }
            }
            Text(stringResource(R.string.month_starts, s.startDay), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            androidx.compose.material3.Slider(
                value = s.startDay.toFloat(), onValueChange = { v -> vm.updateSettings { it.copy(startDay = v.toInt()) } },
                valueRange = 1f..28f, steps = 26,
            )
            LinkRow(stringResource(R.string.accounts_categories), chevron = true, onClick = onOpenManage)
            LinkRow(stringResource(R.string.goals_link), chevron = true, onClick = onOpenGoals)
            LinkRow(stringResource(R.string.people), chevron = true, onClick = onOpenPeople)
        }

        Group(stringResource(R.string.notifications)) {
            ToggleRow(stringResource(R.string.daily_reminder), s.dailyReminder) { on ->
                if (on) requestNotificationsIfNeeded()
                vm.updateSettings { it.copy(dailyReminder = on) }
            }
            if (s.dailyReminder) {
                LinkRow(stringResource(R.string.reminder_time, "%02d:%02d".format(s.reminderHour, s.reminderMinute))) { pickTime = true }
            }
            ToggleRow(stringResource(R.string.bill_reminders), s.billReminders) { on ->
                if (on) requestNotificationsIfNeeded()
                vm.updateSettings { it.copy(billReminders = on) }
            }
            if (s.billReminders) {
                LinkRow(stringResource(R.string.bill_reminder_time, "%02d:%02d".format(s.billHour, s.billMinute))) { pickBillTime = true }
                LinkRow(stringResource(R.string.send_bill_reminders_now)) {
                    scope.launch { message = context.resources.getString(R.string.bill_check_done, vm.checkBillsNow()) }
                }
                Text(stringResource(R.string.bill_reminders_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            ToggleRow(stringResource(R.string.budget_alerts), s.budgetAlerts) { on ->
                if (on) requestNotificationsIfNeeded()
                vm.updateSettings { it.copy(budgetAlerts = on) }
            }
        }

        Group(stringResource(R.string.security)) {
            val secure = keyguard.isDeviceSecure
            ToggleRow(stringResource(R.string.app_lock), s.appLock, enabled = secure) { on -> vm.updateSettings { it.copy(appLock = on) } }
            Text(
                stringResource(if (secure) R.string.app_lock_hint else R.string.app_lock_needs_screen_lock),
                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
            )
        }

        Group(stringResource(R.string.your_data)) {
            LinkRow(stringResource(R.string.export_backup)) { exportJson.launch("furibudget-backup-${LocalDate.now()}.json") }
            LinkRow(stringResource(R.string.export_csv)) { exportCsv.launch("furibudget-transactions-${LocalDate.now()}.csv") }
            LinkRow(stringResource(R.string.import_backup)) { importFile.launch(arrayOf("application/json", "text/plain", "*/*")) }
            LinkRow(stringResource(R.string.load_sample)) { vm.loadSample(); message = msg_sample_loaded }
            LinkRow(stringResource(R.string.recently_deleted), chevron = true, onClick = onOpenTrash)
            LinkRow(stringResource(R.string.erase_all), danger = true) { confirmWipe = true }
        }

        Group(stringResource(R.string.about)) {
            Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        message?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = c.accentText) }
    }

    confirmImport?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.import_backup)) },
            text = { Text(stringResource(R.string.import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = null
                    scope.launch {
                        val file = vm.importJson(text)
                        file?.theme?.let { t -> onTheme { t.copy(photoPath = null, background = t.background.takeIf { it != app.furibudget.ui.theme.BackgroundKind.PHOTO }) } }
                        message = if (file != null) msg_backup_restored else msg_backup_invalid
                    }
                }) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.erase_all)) },
            text = { Text(stringResource(R.string.erase_warning)) },
            confirmButton = { TextButton(onClick = { confirmWipe = false; vm.wipeAll() }) { Text(stringResource(R.string.erase), color = Furi.colors.expense) } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (pickBillTime) {
        val t = rememberTimePickerState(s.billHour, s.billMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickBillTime = false },
            confirmButton = { TextButton(onClick = { vm.updateSettings { it.copy(billHour = t.hour, billMinute = t.minute) }; pickBillTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickBillTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(t) },
        )
    }
    if (pickTime) {
        val t = rememberTimePickerState(s.reminderHour, s.reminderMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = { vm.updateSettings { it.copy(reminderHour = t.hour, reminderMinute = t.minute) }; pickTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(t) },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Furi.colors.onSurface)
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
private fun LinkRow(label: String, danger: Boolean = false, chevron: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (danger) Furi.colors.expense else Furi.colors.onSurface)
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Furi.colors.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Furi.colors.onSurface)
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}
