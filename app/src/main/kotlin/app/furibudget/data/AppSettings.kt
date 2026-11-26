package app.furibudget.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")

data class AppSettings(
    val currencyCode: String = "PHP",
    /** First day of the budget month, 1 to 28. */
    val startDay: Int = 1,
    val onboarded: Boolean = false,
    val appLock: Boolean = false,
    val dailyReminder: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val budgetAlerts: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val currency = stringPreferencesKey("currency")
        val startDay = intPreferencesKey("start_day")
        val onboarded = booleanPreferencesKey("onboarded")
        val lock = booleanPreferencesKey("app_lock")
        val reminder = booleanPreferencesKey("reminder")
        val hour = intPreferencesKey("reminder_hour")
        val minute = intPreferencesKey("reminder_minute")
        val alerts = booleanPreferencesKey("budget_alerts")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            p[K.currency] ?: d.currencyCode, p[K.startDay] ?: d.startDay, p[K.onboarded] ?: d.onboarded, p[K.lock] ?: d.appLock,
            p[K.reminder] ?: d.dailyReminder, p[K.hour] ?: d.reminderHour, p[K.minute] ?: d.reminderMinute, p[K.alerts] ?: d.budgetAlerts,
        )
    }

    suspend fun update(change: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { p ->
            val cur = AppSettings(
                p[K.currency] ?: "PHP", p[K.startDay] ?: 1, p[K.onboarded] ?: false, p[K.lock] ?: false,
                p[K.reminder] ?: false, p[K.hour] ?: 20, p[K.minute] ?: 0, p[K.alerts] ?: true,
            )
            val n = change(cur)
            p[K.currency] = n.currencyCode; p[K.startDay] = n.startDay.coerceIn(1, 28); p[K.onboarded] = n.onboarded
            p[K.lock] = n.appLock; p[K.reminder] = n.dailyReminder; p[K.hour] = n.reminderHour
            p[K.minute] = n.reminderMinute; p[K.alerts] = n.budgetAlerts
        }
    }
}
