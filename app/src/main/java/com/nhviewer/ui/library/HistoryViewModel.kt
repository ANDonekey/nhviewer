package com.nhviewer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    val history: StateFlow<List<GallerySummary>> = libraryRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            libraryRepository.clearHistory()
        }
    }

    fun removeByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { libraryRepository.removeHistory(it) }
        }
    }
}
