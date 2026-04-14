package com.nhviewer.domain.repository

import com.nhviewer.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setImageQuality(value: String)
    suspend fun setMaxConcurrency(value: Int)
    suspend fun setThemeMode(value: String)
    suspend fun setLanguage(value: String)
    suspend fun setPreferJapaneseTitle(value: Boolean)
    suspend fun setShowChineseTags(value: Boolean)
    suspend fun setHomeLanguageFilter(value: String)
    suspend fun setHomeSortOption(value: String)
    suspend fun setApiKey(value: String)
    suspend fun setSplashAnimationEnabled(value: Boolean) {}
    suspend fun setHideBlacklisted(value: Boolean)
    suspend fun setFavoritesSource(value: String)
    suspend fun setReaderTapPagingEnabled(value: Boolean)
    suspend fun setReaderSwipePagingEnabled(value: Boolean)
    suspend fun setReaderTapToToggleChromeEnabled(value: Boolean)
    suspend fun setReaderReverseTapZones(value: Boolean)
    suspend fun setReaderGestureEnabled(value: Boolean)
    suspend fun setReaderLeftHandedMode(value: Boolean)
    suspend fun setReaderPagingMode(value: String)
}
