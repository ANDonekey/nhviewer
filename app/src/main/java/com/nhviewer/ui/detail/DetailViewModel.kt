package com.nhviewer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.ReaderProgressRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetGalleryCommentsUseCase
import com.nhviewer.domain.usecase.GetGalleryDetailUseCase
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val getGalleryDetailUseCase: GetGalleryDetailUseCase,
    private val getGalleryCommentsUseCase: GetGalleryCommentsUseCase,
    private val galleryRepository: GalleryRepository,
    private val libraryRepository: LibraryRepository,
    private val readerProgressRepository: ReaderProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    val settings = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nhviewer.domain.model.AppSettings())
    private var favoriteObserveJob: Job? = null
    private var settingsObserveJob: Job? = null
    private var useOnlineFavorites: Boolean = false

    fun loadDetail(galleryId: Long) {
        _uiState.update {
            it.copy(
                galleryId = galleryId,
                detailState = LoadState.Loading,
                commentsState = LoadState.Loading
            )
        }

        settingsObserveJob?.cancel()
        settingsObserveJob = viewModelScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                useOnlineFavorites = settings.favoritesSource.equals("online", ignoreCase = true)
                bindFavoriteState(galleryId)
            }
        }

        viewModelScope.launch {
            readerProgressRepository.observeProgress(galleryId).collect { page ->
                _uiState.update { state -> state.copy(savedProgress = page ?: 0) }
            }
        }

        viewModelScope.launch {
            val result = getGalleryDetailUseCase(galleryId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.update {
                        it.copy(
                            galleryId = galleryId,
                            detailState = LoadState.Content(detail)
                        )
                    }
                    libraryRepository.upsertHistory(
                        GallerySummary(
                            id = detail.id,
                            title = detail.title,
                            coverUrl = detail.coverUrl,
                            pageCount = detail.pageCount,
                            tags = detail.tags
                        ),
                        0
                    )
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            galleryId = galleryId,
                            detailState = LoadState.Error(
                                throwable.message ?: "Load detail failed"
                            )
                        )
                    }
                }
            )
        }

        viewModelScope.launch {
            val result = getGalleryCommentsUseCase(galleryId)
            result.fold(
                onSuccess = { comments ->
                    _uiState.update {
                        it.copy(
                            commentsState = if (comments.isEmpty()) {
                                LoadState.Empty
                            } else {
                                LoadState.Content(comments)
                            }
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            commentsState = LoadState.Error(
                                throwable.message ?: "Load comments failed"
                            )
                        )
                    }
                }
            )
        }
    }

    fun toggleFavorite() {
        val content = (_uiState.value.detailState as? LoadState.Content)?.value ?: return
        viewModelScope.launch {
            if (useOnlineFavorites) {
                val action = if (_uiState.value.isFavorite) {
                    galleryRepository.removeFavoriteOnline(content.id)
                } else {
                    galleryRepository.addFavoriteOnline(content.id)
                }
                action.fold(
                    onSuccess = { favorite ->
                        _uiState.update { it.copy(isFavorite = favorite) }
                    },
                    onFailure = { _ -> Unit }
                )
            } else {
                if (_uiState.value.isFavorite) {
                    libraryRepository.removeFavorite(content.id)
                } else {
                    libraryRepository.addFavorite(
                        GallerySummary(
                            id = content.id,
                            title = content.title,
                            coverUrl = content.coverUrl,
                            pageCount = content.pageCount,
                            tags = content.tags
                        )
                    )
                }
            }
        }
    }

    fun saveReadingProgress(pageIndex: Int) {
        val content = (_uiState.value.detailState as? LoadState.Content)?.value ?: return
        viewModelScope.launch {
            readerProgressRepository.saveProgress(content.id, pageIndex)
        }
    }

    private fun bindFavoriteState(galleryId: Long) {
        favoriteObserveJob?.cancel()
        favoriteObserveJob = viewModelScope.launch {
            if (useOnlineFavorites) {
                val result = galleryRepository.checkFavorite(galleryId)
                result.fold(
                    onSuccess = { isFavorite -> _uiState.update { it.copy(isFavorite = isFavorite) } },
                    onFailure = { _uiState.update { it.copy(isFavorite = false) } }
                )
            } else {
                libraryRepository.observeIsFavorite(galleryId).collectLatest { isFavorite ->
                    _uiState.update { state -> state.copy(isFavorite = isFavorite) }
                }
            }
        }
    }
}
