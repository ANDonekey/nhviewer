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
    suspend fun setHomeLanguageFilter(value: String)
    suspend fun setHomeSortOption(value: String)
    suspend fun setApiKey(value: String)
    suspend fun setHideBlacklisted(value: Boolean)
    suspend fun setFavoritesSource(value: String)
}
