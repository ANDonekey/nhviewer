package com.nhviewer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.usecase.GetAllGalleriesUseCase
import com.nhviewer.domain.usecase.GetPopularGalleriesUseCase
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getAllGalleriesUseCase: GetAllGalleriesUseCase,
    private val getPopularGalleriesUseCase: GetPopularGalleriesUseCase,
    private val searchGalleriesUseCase: SearchGalleriesUseCase,
    private val searchTagsUseCase: SearchTagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val languageTagCache = mutableMapOf<HomeLanguageFilter, Tag>()

    fun setLanguageFilter(filter: HomeLanguageFilter) {
        if (_uiState.value.languageFilter == filter) return
        _uiState.update { it.copy(languageFilter = filter) }
        loadHome(1)
    }

    fun setSortOption(sortOption: SortOption) {
        if (_uiState.value.sortOption == sortOption) return
        _uiState.update { it.copy(sortOption = sortOption) }
        loadHome(1)
    }

    fun setHideBlacklisted(value: Boolean) {
        if (_uiState.value.hideBlacklisted == value) return
        _uiState.update { it.copy(hideBlacklisted = value) }
        loadHome(1)
    }

    fun loadHome(page: Int = 1) {
        val filter = _uiState.value.languageFilter
        val sort = _uiState.value.sortOption
        _uiState.update { it.copy(galleryListState = LoadState.Loading, currentPage = page) }
        viewModelScope.launch {
            val result = when (filter) {
                HomeLanguageFilter.ALL -> {
                    when (sort) {
                        SortOption.RECENT -> getAllGalleriesUseCase(page)
                        SortOption.POPULAR -> getPopularGalleriesUseCase(page)
                        SortOption.RANDOM -> getAllGalleriesUseCase(page).map { it.shuffleItems() }
                    }
                }
                HomeLanguageFilter.JAPANESE -> {
                    val tag = resolveLanguageTag(HomeLanguageFilter.JAPANESE)
                    searchGalleriesUseCase(
                        query = if (tag == null) "japanese" else "",
                        page = page,
                        sort = if (sort == SortOption.RANDOM) SortOption.RECENT else sort,
                        tags = tag?.let { listOf(it) } ?: emptyList()
                    ).let { base -> if (sort == SortOption.RANDOM) base.map { it.shuffleItems() } else base }
                }
                HomeLanguageFilter.CHINESE -> {
                    val tag = resolveLanguageTag(HomeLanguageFilter.CHINESE)
                    searchGalleriesUseCase(
                        query = if (tag == null) "chinese" else "",
                        page = page,
                        sort = if (sort == SortOption.RANDOM) SortOption.RECENT else sort,
                        tags = tag?.let { listOf(it) } ?: emptyList()
                    ).let { base -> if (sort == SortOption.RANDOM) base.map { it.shuffleItems() } else base }
                }
            }

            result.fold(
                onSuccess = { pageData ->
                    val filteredItems = if (_uiState.value.hideBlacklisted) {
                        pageData.items.filterNot { it.blacklisted }
                    } else {
                        pageData.items
                    }
                    val contentState = if (filteredItems.isEmpty()) {
                        LoadState.Empty
                    } else {
                        LoadState.Content(filteredItems)
                    }
                    _uiState.update { state ->
                        state.copy(
                            galleryListState = contentState,
                            currentPage = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1)
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(galleryListState = LoadState.Error(throwable.message ?: "Unknown error"))
                    }
                }
            )
        }
    }

    private fun Page<GallerySummary>.shuffleItems(): Page<GallerySummary> {
        return copy(items = items.shuffled())
    }

    private suspend fun resolveLanguageTag(filter: HomeLanguageFilter): Tag? {
        languageTagCache[filter]?.let { return it }

        val keyword = when (filter) {
            HomeLanguageFilter.JAPANESE -> "japanese"
            HomeLanguageFilter.CHINESE -> "chinese"
            HomeLanguageFilter.ALL -> return null
        }
        val tags = searchTagsUseCase(keyword).getOrDefault(emptyList())
        val matched = tags.firstOrNull {
            it.type.equals("language", ignoreCase = true) && it.name.equals(keyword, ignoreCase = true)
        } ?: tags.firstOrNull { it.type.equals("language", ignoreCase = true) }

        if (matched != null) {
            languageTagCache[filter] = matched
        }
        return matched
    }
}
