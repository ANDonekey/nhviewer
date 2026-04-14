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
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore(
    private val produceFile: () -> File
) : SettingsRepository {
    constructor(context: Context) : this(
        produceFile = { context.preferencesDataStoreFile(FILE_NAME) }
    )

    internal constructor(file: File) : this(
        produceFile = { file }
    )

    private val dataStore = PreferenceDataStoreFactory.create {
        produceFile()
    }

    override fun observeSettings(): Flow<AppSettings> {
        return dataStore.data.map { prefs ->
            AppSettings(
                imageQuality = prefs[KEY_IMAGE_QUALITY] ?: "high",
                maxConcurrency = prefs[KEY_MAX_CONCURRENCY] ?: 3,
                themeMode = prefs[KEY_THEME_MODE] ?: "system",
                language = prefs[KEY_LANGUAGE] ?: "zh-CN",
                preferJapaneseTitle = prefs[KEY_PREFER_JAPANESE_TITLE] ?: false,
                showChineseTags = prefs[KEY_SHOW_CHINESE_TAGS] ?: true,
                homeLanguageFilter = prefs[KEY_HOME_LANGUAGE_FILTER] ?: "all",
                homeSortOption = prefs[KEY_HOME_SORT_OPTION] ?: "recent",
                apiKey = prefs[KEY_API_KEY] ?: "",
                splashAnimationEnabled = prefs[KEY_SPLASH_ANIMATION_ENABLED] ?: true,
                hideBlacklisted = prefs[KEY_HIDE_BLACKLISTED] ?: false,
                favoritesSource = prefs[KEY_FAVORITES_SOURCE] ?: "local",
                readerTapPagingEnabled = prefs[KEY_READER_TAP_PAGING_ENABLED] ?: true,
                readerSwipePagingEnabled = prefs[KEY_READER_SWIPE_PAGING_ENABLED] ?: true,
                readerTapToToggleChromeEnabled = prefs[KEY_READER_TAP_TOGGLE_CHROME_ENABLED] ?: true,
                readerReverseTapZones = prefs[KEY_READER_REVERSE_TAP_ZONES] ?: false,
                readerGestureEnabled = prefs[KEY_READER_GESTURE_ENABLED] ?: true,
                readerLeftHandedMode = prefs[KEY_READER_LEFT_HANDED_MODE] ?: false,
                readerPagingMode = prefs[KEY_READER_PAGING_MODE] ?: "single"
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

    override suspend fun setShowChineseTags(value: Boolean) {
        dataStore.edit { it[KEY_SHOW_CHINESE_TAGS] = value }
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

    override suspend fun setSplashAnimationEnabled(value: Boolean) {
        dataStore.edit { it[KEY_SPLASH_ANIMATION_ENABLED] = value }
    }

    override suspend fun setHideBlacklisted(value: Boolean) {
        dataStore.edit { it[KEY_HIDE_BLACKLISTED] = value }
    }

    override suspend fun setFavoritesSource(value: String) {
        dataStore.edit { it[KEY_FAVORITES_SOURCE] = value }
    }

    override suspend fun setReaderTapPagingEnabled(value: Boolean) {
        dataStore.edit { it[KEY_READER_TAP_PAGING_ENABLED] = value }
    }

    override suspend fun setReaderSwipePagingEnabled(value: Boolean) {
        dataStore.edit { it[KEY_READER_SWIPE_PAGING_ENABLED] = value }
    }

    override suspend fun setReaderTapToToggleChromeEnabled(value: Boolean) {
        dataStore.edit { it[KEY_READER_TAP_TOGGLE_CHROME_ENABLED] = value }
    }

    override suspend fun setReaderReverseTapZones(value: Boolean) {
        dataStore.edit { it[KEY_READER_REVERSE_TAP_ZONES] = value }
    }

    override suspend fun setReaderGestureEnabled(value: Boolean) {
        dataStore.edit { it[KEY_READER_GESTURE_ENABLED] = value }
    }

    override suspend fun setReaderLeftHandedMode(value: Boolean) {
        dataStore.edit { it[KEY_READER_LEFT_HANDED_MODE] = value }
    }

    override suspend fun setReaderPagingMode(value: String) {
        val normalized = when (value.lowercase()) {
            "continuous" -> "continuous"
            else -> "single"
        }
        dataStore.edit { it[KEY_READER_PAGING_MODE] = normalized }
    }

    companion object {
        private const val FILE_NAME = "nhviewer_settings.preferences_pb"
        private val KEY_IMAGE_QUALITY = stringPreferencesKey("image_quality")
        private val KEY_MAX_CONCURRENCY = intPreferencesKey("max_concurrency")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_PREFER_JAPANESE_TITLE = booleanPreferencesKey("prefer_japanese_title")
        private val KEY_SHOW_CHINESE_TAGS = booleanPreferencesKey("show_chinese_tags")
        private val KEY_HOME_LANGUAGE_FILTER = stringPreferencesKey("home_language_filter")
        private val KEY_HOME_SORT_OPTION = stringPreferencesKey("home_sort_option")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_SPLASH_ANIMATION_ENABLED = booleanPreferencesKey("splash_animation_enabled")
        private val KEY_HIDE_BLACKLISTED = booleanPreferencesKey("hide_blacklisted")
        private val KEY_FAVORITES_SOURCE = stringPreferencesKey("favorites_source")
        private val KEY_READER_TAP_PAGING_ENABLED = booleanPreferencesKey("reader_tap_paging_enabled")
        private val KEY_READER_SWIPE_PAGING_ENABLED = booleanPreferencesKey("reader_swipe_paging_enabled")
        private val KEY_READER_TAP_TOGGLE_CHROME_ENABLED = booleanPreferencesKey("reader_tap_toggle_chrome_enabled")
        private val KEY_READER_REVERSE_TAP_ZONES = booleanPreferencesKey("reader_reverse_tap_zones")
        private val KEY_READER_GESTURE_ENABLED = booleanPreferencesKey("reader_gesture_enabled")
        private val KEY_READER_LEFT_HANDED_MODE = booleanPreferencesKey("reader_left_handed_mode")
        private val KEY_READER_PAGING_MODE = stringPreferencesKey("reader_paging_mode")
    }
}
