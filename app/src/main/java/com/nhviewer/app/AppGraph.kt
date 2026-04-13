package com.nhviewer.app

import android.content.Context
import androidx.room.Room
import com.nhviewer.core.network.NetworkClientFactory
import com.nhviewer.core.network.NetworkAuthState
import com.nhviewer.data.local.AppDatabase
import com.nhviewer.data.remote.NhentaiRemoteDataSource
import com.nhviewer.data.repository.GalleryRepositoryImpl
import com.nhviewer.data.repository.LibraryRepositoryImpl
import com.nhviewer.data.repository.ReaderProgressRepositoryImpl
import com.nhviewer.data.settings.SettingsDataStore
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.ReaderProgressRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetAllGalleriesUseCase
import com.nhviewer.domain.usecase.GetPopularGalleriesUseCase
import com.nhviewer.domain.usecase.GetGalleryDetailUseCase
import com.nhviewer.domain.usecase.GetGalleryCommentsUseCase
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object AppGraph {
    private lateinit var appContext: Context
    private var initialized = false
    private var settingsSyncStarted = false
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
        startSettingsSync()
    }

    private fun requireInit() {
        check(initialized) { "AppGraph is not initialized. Call AppGraph.init(...) first." }
    }

    private val nhentaiService by lazy { NetworkClientFactory.createNhentaiService() }
    private val remoteDataSource by lazy { NhentaiRemoteDataSource(nhentaiService) }
    private val database by lazy {
        requireInit()
        Room.databaseBuilder(appContext, AppDatabase::class.java, "nhviewer.db").build()
    }

    val galleryRepository: GalleryRepository by lazy {
        GalleryRepositoryImpl(remoteDataSource)
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepositoryImpl(database.favoriteDao(), database.historyDao())
    }

    val readerProgressRepository: ReaderProgressRepository by lazy {
        ReaderProgressRepositoryImpl(database.readingProgressDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        requireInit()
        SettingsDataStore(appContext)
    }

    private fun startSettingsSync() {
        if (settingsSyncStarted) return
        settingsSyncStarted = true
        appScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                NetworkAuthState.apiKey = settings.apiKey.ifBlank { null }
            }
        }
    }

    val getPopularGalleriesUseCase: GetPopularGalleriesUseCase by lazy {
        GetPopularGalleriesUseCase(galleryRepository)
    }

    val getAllGalleriesUseCase: GetAllGalleriesUseCase by lazy {
        GetAllGalleriesUseCase(galleryRepository)
    }

    val getGalleryDetailUseCase: GetGalleryDetailUseCase by lazy {
        GetGalleryDetailUseCase(galleryRepository)
    }

    val getGalleryCommentsUseCase: GetGalleryCommentsUseCase by lazy {
        GetGalleryCommentsUseCase(galleryRepository)
    }

    val searchGalleriesUseCase: SearchGalleriesUseCase by lazy {
        SearchGalleriesUseCase(galleryRepository)
    }

    val searchTagsUseCase: SearchTagsUseCase by lazy {
        SearchTagsUseCase(galleryRepository)
    }
}
