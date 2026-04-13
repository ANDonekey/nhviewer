package com.nhviewer.app

import androidx.appcompat.app.AppCompatDelegate

object ThemeController {
    fun apply(themeMode: String) {
        val mode = when (themeMode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
