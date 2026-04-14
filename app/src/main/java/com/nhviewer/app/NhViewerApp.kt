package com.nhviewer.app

import android.app.Application
import android.util.Log
import com.nhviewer.core.network.NetworkAuthState
import com.nhviewer.data.local.bootstrap.TagCatalogBootstrapper
import com.nhviewer.di.nhViewerModules
import com.nhviewer.download.DownloadCenter
import com.nhviewer.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class NhViewerApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NhViewerApp)
            modules(nhViewerModules())
        }
        val tagCatalogBootstrapper = GlobalContext.get().get<TagCatalogBootstrapper>()
        val settingsRepository = GlobalContext.get().get<SettingsRepository>()
        DownloadCenter.init(this)
        appScope.launch(Dispatchers.IO) {
            runCatching { tagCatalogBootstrapper.ensureSeeded() }
                .onFailure { Log.e("NhViewerApp", "Failed to initialize tag catalog", it) }
        }
        appScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                ThemeController.apply(settings.themeMode)
                NetworkAuthState.apiKey = settings.apiKey.ifBlank { null }
            }
        }
    }
}
