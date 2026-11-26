package app.furibudget.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.themeStore by preferencesDataStore("theme")

class ThemeRepository(private val context: Context) {
    private val key = stringPreferencesKey("spec")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val spec: Flow<ThemeSpec> = context.themeStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<ThemeSpec>(it) }.getOrNull() } ?: ThemeSpec()
    }

    suspend fun save(spec: ThemeSpec) {
        context.themeStore.edit { it[key] = json.encodeToString(spec) }
    }
}
