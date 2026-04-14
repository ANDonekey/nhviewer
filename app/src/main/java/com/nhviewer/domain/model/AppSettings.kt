package com.nhviewer.domain.model

data class AppSettings(
    val imageQuality: String = "high",
    val maxConcurrency: Int = 3,
    val themeMode: String = "system",
    val language: String = "zh-CN",
    val preferJapaneseTitle: Boolean = false,
    val showChineseTags: Boolean = true,
    val homeLanguageFilter: String = "all",
    val homeSortOption: String = "recent",
    val apiKey: String = "",
    val splashAnimationEnabled: Boolean = true,
    val hideBlacklisted: Boolean = false,
    val favoritesSource: String = "local",
    val readerTapPagingEnabled: Boolean = true,
    val readerSwipePagingEnabled: Boolean = true,
    val readerTapToToggleChromeEnabled: Boolean = true,
    val readerReverseTapZones: Boolean = false,
    val readerGestureEnabled: Boolean = true,
    val readerLeftHandedMode: Boolean = false,
    val readerPagingMode: String = "single"
)
