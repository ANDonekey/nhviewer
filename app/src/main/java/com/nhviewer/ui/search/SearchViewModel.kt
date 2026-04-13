package com.nhviewer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchGalleriesUseCase: SearchGalleriesUseCase,
    private val searchTagsUseCase: SearchTagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateKeyword(keyword: String) {
        _uiState.update { state ->
            state.copy(queryState = state.queryState.copy(keyword = keyword))
        }
    }

    fun updateSort(sortOption: SortOption) {
        _uiState.update { state ->
            state.copy(queryState = state.queryState.copy(sortOption = sortOption))
        }
    }

    fun updateTags(tags: List<Tag>) {
        _uiState.update { state ->
            state.copy(queryState = state.queryState.copy(selectedTags = tags))
        }
    }

    fun addTagByKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val result = searchTagsUseCase(trimmed)
            result.fold(
                onSuccess = { tags ->
                    val first = tags.firstOrNull()
                    if (first == null) {
                        _uiState.update { it.copy(tagMessage = "No matching tag: $trimmed") }
                        return@fold
                    }
                    val current = _uiState.value.queryState.selectedTags
                    val merged = (current + first).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            queryState = it.queryState.copy(selectedTags = merged),
                            tagMessage = "Added tag: ${first.type}/${first.name}"
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(tagMessage = throwable.message ?: "Tag search failed") }
                }
            )
        }
    }

    fun clearTags() {
        _uiState.update {
            it.copy(queryState = it.queryState.copy(selectedTags = emptyList()), tagMessage = "Tags cleared")
        }
    }

    fun search(page: Int = 1) {
        val query = uiState.value.queryState
        _uiState.update {
            it.copy(
                queryState = query.copy(page = page),
                resultState = LoadState.Loading
            )
        }

        viewModelScope.launch {
            val result = searchGalleriesUseCase(
                query = query.keyword,
                page = page,
                sort = query.sortOption,
                tags = query.selectedTags
            )

            result.fold(
                onSuccess = { pageData ->
                    val loadState = if (pageData.items.isEmpty()) {
                        LoadState.Empty
                    } else {
                        LoadState.Content(pageData.items)
                    }
                    _uiState.update { state ->
                        state.copy(
                            queryState = state.queryState.copy(page = pageData.page),
                            resultState = loadState,
                            totalPages = pageData.totalPages
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            resultState = LoadState.Error(
                                throwable.message ?: "Search failed"
                            )
                        )
                    }
                }
            )
        }
    }
}
