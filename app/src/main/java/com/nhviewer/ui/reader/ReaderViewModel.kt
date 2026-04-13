package com.nhviewer.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.usecase.GetGalleryDetailUseCase
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.ReaderProgressRepository
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val getGalleryDetailUseCase: GetGalleryDetailUseCase,
    private val readerProgressRepository: ReaderProgressRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun load(galleryId: Long) {
        _uiState.update { it.copy(galleryId = galleryId, detailState = LoadState.Loading) }

        viewModelScope.launch {
            readerProgressRepository.observeProgress(galleryId).collect { progress ->
                _uiState.update { it.copy(savedProgress = progress ?: 0) }
            }
        }

        viewModelScope.launch {
            val result = getGalleryDetailUseCase(galleryId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.update { it.copy(detailState = LoadState.Content(detail)) }
                    libraryRepository.upsertHistory(
                        GallerySummary(
                            id = detail.id,
                            title = detail.title,
                            coverUrl = detail.coverUrl,
                            pageCount = detail.pageCount,
                            tags = detail.tags
                        ),
                        _uiState.value.savedProgress
                    )
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(detailState = LoadState.Error(throwable.message ?: "Reader load failed")) }
                }
            )
        }
    }

    fun saveProgress(galleryId: Long, pageIndex: Int, title: String, coverUrl: String?, pageCount: Int) {
        viewModelScope.launch {
            readerProgressRepository.saveProgress(galleryId, pageIndex)
            libraryRepository.upsertHistory(
                GallerySummary(
                    id = galleryId,
                    title = title,
                    coverUrl = coverUrl,
                    pageCount = pageCount,
                    tags = emptyList()
                ),
                pageIndex
            )
        }
    }
}
