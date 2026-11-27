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
    val billReminders: Boolean = true,
    val billHour: Int = 9,
    val billMinute: Int = 0,
    /** Off by default. When on, the app may fetch exchange rates; only currency codes leave the phone. */
    val ratesOnline: Boolean = false,
    val ratesFetchedAt: Long = 0L,
    val soundOn: Boolean = false,
    val soundVolume: Int = 70,
    val hapticsOn: Boolean = true,
    /** 0 light, 1 normal, 2 strong. */
    val hapticStrength: Int = 1,
    /** Style per event: soft, pop, coins, water, retro, silent or custom. */
    val soundStyles: Map<String, String> = SoundDefaults,
)

val SoundDefaults: Map<String, String> = mapOf("keypad" to "soft", "saved" to "soft", "paid" to "soft", "deleted" to "soft", "goal" to "soft")

private fun encodeStyles(m: Map<String, String>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }
private fun decodeStyles(s: String?): Map<String, String> =
    if (s.isNullOrBlank()) SoundDefaults else SoundDefaults + s.split(";").mapNotNull { it.split("=").takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }.filter { it.first in SoundDefaults }

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
        val billOn = booleanPreferencesKey("bill_reminders")
        val billHour = intPreferencesKey("bill_hour")
        val billMinute = intPreferencesKey("bill_minute")
        val ratesOnline = booleanPreferencesKey("rates_online")
        val soundOn = booleanPreferencesKey("sound_on")
        val soundVol = intPreferencesKey("sound_volume")
        val hapticsOn = booleanPreferencesKey("haptics_on")
        val hapticStrength = intPreferencesKey("haptic_strength")
        val soundStyles = stringPreferencesKey("sound_styles")
        val ratesAt = androidx.datastore.preferences.core.longPreferencesKey("rates_fetched_at")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            p[K.currency] ?: d.currencyCode, p[K.startDay] ?: d.startDay, p[K.onboarded] ?: d.onboarded, p[K.lock] ?: d.appLock,
            p[K.reminder] ?: d.dailyReminder, p[K.hour] ?: d.reminderHour, p[K.minute] ?: d.reminderMinute, p[K.alerts] ?: d.budgetAlerts,
            p[K.billOn] ?: d.billReminders, p[K.billHour] ?: d.billHour, p[K.billMinute] ?: d.billMinute,
            p[K.ratesOnline] ?: d.ratesOnline, p[K.ratesAt] ?: d.ratesFetchedAt,
            p[K.soundOn] ?: d.soundOn, p[K.soundVol] ?: d.soundVolume, p[K.hapticsOn] ?: d.hapticsOn, p[K.hapticStrength] ?: d.hapticStrength, decodeStyles(p[K.soundStyles]),
        )
    }

    suspend fun update(change: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { p ->
            val cur = AppSettings(
                p[K.currency] ?: "PHP", p[K.startDay] ?: 1, p[K.onboarded] ?: false, p[K.lock] ?: false,
                p[K.reminder] ?: false, p[K.hour] ?: 20, p[K.minute] ?: 0, p[K.alerts] ?: true,
                p[K.billOn] ?: true, p[K.billHour] ?: 9, p[K.billMinute] ?: 0,
                p[K.ratesOnline] ?: false, p[K.ratesAt] ?: 0L,
                p[K.soundOn] ?: false, p[K.soundVol] ?: 70, p[K.hapticsOn] ?: true, p[K.hapticStrength] ?: 1, decodeStyles(p[K.soundStyles]),
            )
            val n = change(cur)
            p[K.currency] = n.currencyCode; p[K.startDay] = n.startDay.coerceIn(1, 28); p[K.onboarded] = n.onboarded
            p[K.lock] = n.appLock; p[K.reminder] = n.dailyReminder; p[K.hour] = n.reminderHour
            p[K.minute] = n.reminderMinute; p[K.alerts] = n.budgetAlerts
            p[K.billOn] = n.billReminders; p[K.billHour] = n.billHour.coerceIn(0, 23); p[K.billMinute] = n.billMinute.coerceIn(0, 59)
            p[K.ratesOnline] = n.ratesOnline; p[K.ratesAt] = n.ratesFetchedAt
            p[K.soundOn] = n.soundOn; p[K.soundVol] = n.soundVolume.coerceIn(0, 100); p[K.hapticsOn] = n.hapticsOn
            p[K.hapticStrength] = n.hapticStrength.coerceIn(0, 2); p[K.soundStyles] = encodeStyles(n.soundStyles)
        }
    }
}
