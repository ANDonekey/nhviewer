package com.nhviewer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.AppSettings
import com.nhviewer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setImageQuality(value: String) = launch { settingsRepository.setImageQuality(value) }
    fun setMaxConcurrency(value: Int) = launch { settingsRepository.setMaxConcurrency(value) }
    fun setThemeMode(value: String) = launch { settingsRepository.setThemeMode(value) }
    fun setLanguage(value: String) = launch { settingsRepository.setLanguage(value) }
    fun setPreferJapaneseTitle(value: Boolean) = launch { settingsRepository.setPreferJapaneseTitle(value) }
    fun setHomeLanguageFilter(value: String) = launch { settingsRepository.setHomeLanguageFilter(value) }
    fun setHomeSortOption(value: String) = launch { settingsRepository.setHomeSortOption(value) }
    fun setApiKey(value: String) = launch { settingsRepository.setApiKey(value) }
    fun setHideBlacklisted(value: Boolean) = launch { settingsRepository.setHideBlacklisted(value) }
    fun setFavoritesSource(value: String) = launch { settingsRepository.setFavoritesSource(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
