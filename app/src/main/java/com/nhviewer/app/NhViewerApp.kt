package com.nhviewer.app

import android.app.Application
import com.nhviewer.download.DownloadCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NhViewerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        DownloadCenter.init(this)
        appScope.launch {
            AppGraph.settingsRepository.observeSettings().collectLatest { settings ->
                ThemeController.apply(settings.themeMode)
            }
        }
    }
}
