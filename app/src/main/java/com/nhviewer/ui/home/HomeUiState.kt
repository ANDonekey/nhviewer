package com.nhviewer.ui.home

import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.SortOption
import com.nhviewer.ui.common.LoadState

data class HomeUiState(
    val galleryListState: LoadState<List<GallerySummary>> = LoadState.Loading,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val languageFilter: HomeLanguageFilter = HomeLanguageFilter.ALL,
    val sortOption: SortOption = SortOption.RECENT,
    val hideBlacklisted: Boolean = false
)

enum class HomeLanguageFilter {
    ALL,
    JAPANESE,
    CHINESE
}
