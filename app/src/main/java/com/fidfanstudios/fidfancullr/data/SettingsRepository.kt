package com.fidfanstudios.fidfancullr.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fidfancullr_settings")

/**
 * Persists app settings with DataStore. Destinations are encoded as a simple
 * pipe/semicolon delimited string to avoid pulling in a JSON dependency:
 *   id,label,folderName,gestureKey;id2,label2,folderName2,gestureKey2
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val INBOX_URI = stringPreferencesKey("inbox_uri")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val LANGUAGE = stringPreferencesKey("language")
        val DESTINATIONS = stringPreferencesKey("destinations")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            inboxUri = prefs[Keys.INBOX_URI],
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accentPalette = prefs[Keys.ACCENT]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
                ?: AccentPalette.VIOLET_PIXEL,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "en",
            destinations = prefs[Keys.DESTINATIONS]?.let { decodeDestinations(it) } ?: DEFAULT_DESTINATIONS,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun setInboxUri(uri: String) {
        context.dataStore.edit { it[Keys.INBOX_URI] = uri }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccent(accent: AccentPalette) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setDestinations(destinations: List<SortDestination>) {
        context.dataStore.edit { it[Keys.DESTINATIONS] = encodeDestinations(destinations) }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    private fun encodeDestinations(list: List<SortDestination>): String =
        list.joinToString(";") { "${it.id},${it.label},${it.folderName},${it.gestureKey}" }

    private fun decodeDestinations(raw: String): List<SortDestination> =
        raw.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 4) SortDestination(parts[0], parts[1], parts[2], parts[3]) else null
        }.ifEmpty { DEFAULT_DESTINATIONS }
}
