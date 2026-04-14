package com.nhviewer.di

import androidx.room.Room
import com.nhviewer.core.network.NetworkClientFactory
import com.nhviewer.data.local.AppDatabase
import com.nhviewer.data.local.bootstrap.TagCatalogBootstrapper
import com.nhviewer.data.remote.NhentaiRemoteDataSource
import com.nhviewer.data.remote.NhentaiService
import com.nhviewer.data.repository.GalleryRepositoryImpl
import com.nhviewer.data.repository.LibraryRepositoryImpl
import com.nhviewer.data.repository.ReaderProgressRepositoryImpl
import com.nhviewer.data.settings.SettingsDataStore
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.ReaderProgressRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetAllGalleriesUseCase
import com.nhviewer.domain.usecase.GetGalleryCommentsUseCase
import com.nhviewer.domain.usecase.GetGalleryDetailUseCase
import com.nhviewer.domain.usecase.GetPopularGalleriesUseCase
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.ui.detail.DetailViewModel
import com.nhviewer.ui.home.HomeViewModel
import com.nhviewer.ui.library.FavoritesViewModel
import com.nhviewer.ui.library.HistoryViewModel
import com.nhviewer.ui.reader.ReaderViewModel
import com.nhviewer.ui.search.SearchViewModel
import com.nhviewer.ui.settings.SettingsViewModel
import com.nhviewer.ui.user.UserProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val dataModule = module {
    single<NhentaiService> { NetworkClientFactory.createNhentaiService() }
    single { NhentaiRemoteDataSource(service = get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "nhviewer.db"
        )
            .createFromAsset("databases/nhviewer.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    single {
        val db: AppDatabase = get()
        TagCatalogBootstrapper(
            context = androidContext(),
            tagDao = db.tagDao()
        )
    }
    single<GalleryRepository> {
        val db: AppDatabase = get()
        GalleryRepositoryImpl(
            remoteDataSource = get(),
            tagDao = db.tagDao()
        )
    }
    single<LibraryRepository> {
        val db: AppDatabase = get()
        LibraryRepositoryImpl(
            favoriteDao = db.favoriteDao(),
            historyDao = db.historyDao()
        )
    }
    single<ReaderProgressRepository> {
        val db: AppDatabase = get()
        ReaderProgressRepositoryImpl(dao = db.readingProgressDao())
    }
    single<SettingsRepository> { SettingsDataStore(androidContext()) }
}

private val domainModule = module {
    single { GetAllGalleriesUseCase(galleryRepository = get()) }
    single { GetPopularGalleriesUseCase(galleryRepository = get()) }
    single { GetGalleryDetailUseCase(galleryRepository = get()) }
    single { GetGalleryCommentsUseCase(galleryRepository = get()) }
    single { SearchGalleriesUseCase(galleryRepository = get()) }
    single { SearchTagsUseCase(galleryRepository = get()) }
}

private val homeModule = module {
    viewModel {
        HomeViewModel(
            getAllGalleriesUseCase = get(),
            getPopularGalleriesUseCase = get(),
            searchGalleriesUseCase = get(),
            searchTagsUseCase = get(),
            libraryRepository = get(),
            settingsRepository = get()
        )
    }
}

private val readerModule = module {
    viewModel {
        ReaderViewModel(
            getGalleryDetailUseCase = get(),
            readerProgressRepository = get(),
            libraryRepository = get()
        )
    }
}

private val favoritesModule = module {
    viewModel {
        FavoritesViewModel(
            libraryRepository = get(),
            galleryRepository = get(),
            settingsRepository = get()
        )
    }
}

private val detailModule = module {
    viewModel {
        DetailViewModel(
            getGalleryDetailUseCase = get(),
            getGalleryCommentsUseCase = get(),
            galleryRepository = get(),
            libraryRepository = get(),
            readerProgressRepository = get(),
            settingsRepository = get()
        )
    }
}

private val searchModule = module {
    viewModel {
        SearchViewModel(
            searchGalleriesUseCase = get(),
            searchTagsUseCase = get()
        )
    }
}

private val historyModule = module {
    viewModel { HistoryViewModel(libraryRepository = get()) }
}

private val settingsModule = module {
    viewModel { SettingsViewModel(settingsRepository = get()) }
}

private val userModule = module {
    viewModel { UserProfileViewModel(galleryRepository = get()) }
}

fun nhViewerModules() = listOf(
    dataModule,
    domainModule,
    homeModule,
    readerModule,
    favoritesModule,
    detailModule,
    searchModule,
    historyModule,
    settingsModule,
    userModule
)
