package com.nhviewer.ui.search

import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.ui.common.LoadState

data class SearchUiState(
    val queryState: SearchQueryState = SearchQueryState(),
    val resultState: LoadState<List<GallerySummary>> = LoadState.Empty,
    val totalPages: Int = 1,
    val tagMessage: String? = null
)
