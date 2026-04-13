package com.nhviewer.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nhviewer.app.AppGraph
import com.nhviewer.ui.detail.DetailViewModel
import com.nhviewer.ui.home.HomeViewModel
import com.nhviewer.ui.library.FavoritesViewModel
import com.nhviewer.ui.library.HistoryViewModel
import com.nhviewer.ui.reader.ReaderViewModel
import com.nhviewer.ui.search.SearchViewModel
import com.nhviewer.ui.settings.SettingsViewModel
import com.nhviewer.ui.user.UserProfileViewModel

class NhViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    AppGraph.getAllGalleriesUseCase,
                    AppGraph.getPopularGalleriesUseCase,
                    AppGraph.searchGalleriesUseCase,
                    AppGraph.searchTagsUseCase
                ) as T
            }

            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(
                    AppGraph.searchGalleriesUseCase,
                    AppGraph.searchTagsUseCase
                ) as T
            }

            modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
                DetailViewModel(
                    AppGraph.getGalleryDetailUseCase,
                    AppGraph.getGalleryCommentsUseCase,
                    AppGraph.galleryRepository,
                    AppGraph.libraryRepository,
                    AppGraph.readerProgressRepository,
                    AppGraph.settingsRepository
                ) as T
            }

            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(
                    AppGraph.libraryRepository,
                    AppGraph.galleryRepository,
                    AppGraph.settingsRepository
                ) as T
            }

            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(AppGraph.libraryRepository) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(AppGraph.settingsRepository) as T
            }

            modelClass.isAssignableFrom(UserProfileViewModel::class.java) -> {
                UserProfileViewModel(AppGraph.galleryRepository) as T
            }

            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                ReaderViewModel(
                    AppGraph.getGalleryDetailUseCase,
                    AppGraph.readerProgressRepository,
                    AppGraph.libraryRepository
                ) as T
            }

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
