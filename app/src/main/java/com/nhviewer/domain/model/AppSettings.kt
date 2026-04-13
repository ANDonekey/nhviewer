package com.nhviewer.domain.model

data class AppSettings(
    val imageQuality: String = "high",
    val maxConcurrency: Int = 3,
    val themeMode: String = "system",
    val language: String = "zh-CN",
    val preferJapaneseTitle: Boolean = false,
    val homeLanguageFilter: String = "all",
    val homeSortOption: String = "recent",
    val apiKey: String = "",
    val hideBlacklisted: Boolean = false,
    val favoritesSource: String = "local"
)
