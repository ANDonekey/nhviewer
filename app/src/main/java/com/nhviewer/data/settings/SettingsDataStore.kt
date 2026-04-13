package com.nhviewer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.nhviewer.domain.model.AppSettings
import com.nhviewer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore(
    context: Context
) : SettingsRepository {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("nhviewer_settings.preferences_pb")
    }

    override fun observeSettings(): Flow<AppSettings> {
        return dataStore.data.map { prefs ->
            AppSettings(
                imageQuality = prefs[KEY_IMAGE_QUALITY] ?: "high",
                maxConcurrency = prefs[KEY_MAX_CONCURRENCY] ?: 3,
                themeMode = prefs[KEY_THEME_MODE] ?: "system",
                language = prefs[KEY_LANGUAGE] ?: "zh-CN",
                preferJapaneseTitle = prefs[KEY_PREFER_JAPANESE_TITLE] ?: false,
                homeLanguageFilter = prefs[KEY_HOME_LANGUAGE_FILTER] ?: "all",
                homeSortOption = prefs[KEY_HOME_SORT_OPTION] ?: "recent",
                apiKey = prefs[KEY_API_KEY] ?: "",
                hideBlacklisted = prefs[KEY_HIDE_BLACKLISTED] ?: false,
                favoritesSource = prefs[KEY_FAVORITES_SOURCE] ?: "local"
            )
        }
    }

    override suspend fun setImageQuality(value: String) {
        dataStore.edit { it[KEY_IMAGE_QUALITY] = value }
    }

    override suspend fun setMaxConcurrency(value: Int) {
        dataStore.edit { it[KEY_MAX_CONCURRENCY] = value.coerceIn(1, 8) }
    }

    override suspend fun setThemeMode(value: String) {
        dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    override suspend fun setLanguage(value: String) {
        dataStore.edit { it[KEY_LANGUAGE] = value }
    }

    override suspend fun setPreferJapaneseTitle(value: Boolean) {
        dataStore.edit { it[KEY_PREFER_JAPANESE_TITLE] = value }
    }

    override suspend fun setHomeLanguageFilter(value: String) {
        dataStore.edit { it[KEY_HOME_LANGUAGE_FILTER] = value }
    }

    override suspend fun setHomeSortOption(value: String) {
        dataStore.edit { it[KEY_HOME_SORT_OPTION] = value }
    }

    override suspend fun setApiKey(value: String) {
        dataStore.edit { it[KEY_API_KEY] = value.trim() }
    }

    override suspend fun setHideBlacklisted(value: Boolean) {
        dataStore.edit { it[KEY_HIDE_BLACKLISTED] = value }
    }

    override suspend fun setFavoritesSource(value: String) {
        dataStore.edit { it[KEY_FAVORITES_SOURCE] = value }
    }

    companion object {
        private val KEY_IMAGE_QUALITY = stringPreferencesKey("image_quality")
        private val KEY_MAX_CONCURRENCY = intPreferencesKey("max_concurrency")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_PREFER_JAPANESE_TITLE = booleanPreferencesKey("prefer_japanese_title")
        private val KEY_HOME_LANGUAGE_FILTER = stringPreferencesKey("home_language_filter")
        private val KEY_HOME_SORT_OPTION = stringPreferencesKey("home_sort_option")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_HIDE_BLACKLISTED = booleanPreferencesKey("hide_blacklisted")
        private val KEY_FAVORITES_SOURCE = stringPreferencesKey("favorites_source")
    }
}
