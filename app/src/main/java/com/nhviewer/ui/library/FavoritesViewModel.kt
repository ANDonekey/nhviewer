package com.nhviewer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.ui.common.ErrorText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val source: String = "local",
    val list: List<GallerySummary> = emptyList(),
    val hideBlacklisted: Boolean = false,
    val error: String? = null
)

class FavoritesViewModel(
    private val libraryRepository: LibraryRepository,
    private val galleryRepository: GalleryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var localCollectJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                val source = settings.favoritesSource.lowercase()
                _uiState.update {
                    it.copy(
                        source = source,
                        hideBlacklisted = settings.hideBlacklisted,
                        error = null
                    )
                }
                if (source == "online") {
                    localCollectJob?.cancel()
                    loadOnlineFavorites()
                } else {
                    collectLocalFavorites()
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.source == "online") {
            loadOnlineFavorites()
        }
    }

    fun removeByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            if (_uiState.value.source == "online") {
                ids.forEach { galleryRepository.removeFavoriteOnline(it) }
                loadOnlineFavorites()
            } else {
                ids.forEach { libraryRepository.removeFavorite(it) }
            }
        }
    }

    private fun collectLocalFavorites() {
        localCollectJob?.cancel()
        localCollectJob = viewModelScope.launch {
            libraryRepository.observeFavorites()
                .map { it }
                .collectLatest { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            list = list,
                            error = null
                        )
                    }
                }
        }
    }

    private fun loadOnlineFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = galleryRepository.getFavorites(page = 1)
            result.fold(
                onSuccess = { page ->
                    val filtered = if (_uiState.value.hideBlacklisted) {
                        page.items.filterNot { it.blacklisted }
                    } else {
                        page.items
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            list = filtered,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            list = emptyList(),
                            error = ErrorText.fromMessage(error.message, "Load favorites failed")
                        )
                    }
                }
            )
        }
    }
}
