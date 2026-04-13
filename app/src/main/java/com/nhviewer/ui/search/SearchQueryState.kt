package com.nhviewer.ui.search

import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag

data class SearchQueryState(
    val keyword: String = "",
    val sortOption: SortOption = SortOption.POPULAR,
    val selectedTags: List<Tag> = emptyList(),
    val page: Int = 1
)
