package com.nhviewer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetAllGalleriesUseCase
import com.nhviewer.domain.usecase.GetPopularGalleriesUseCase
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getAllGalleriesUseCase: GetAllGalleriesUseCase,
    private val getPopularGalleriesUseCase: GetPopularGalleriesUseCase,
    private val searchGalleriesUseCase: SearchGalleriesUseCase,
    private val searchTagsUseCase: SearchTagsUseCase,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val languageTagCache = mutableMapOf<HomeLanguageFilter, Tag>()
    private var prefetchedNextPage: Page<GallerySummary>? = null
    private var prefetchedNextPageSourcePage: Int? = null
    private var prefetchedRequestKey: HomeRequestKey? = null
    private var prefetchJob: Job? = null
    val settings = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nhviewer.domain.model.AppSettings())

    /** Persists list scroll position across navigation (survives Activity stop/start). */
    var savedScrollIndex: Int = 0
    var savedScrollOffset: Int = 0

    fun setLanguageFilter(filter: HomeLanguageFilter) {
        if (_uiState.value.languageFilter == filter) return
        clearPrefetchCache()
        _uiState.update { it.copy(languageFilter = filter) }
        loadHome(1)
    }

    fun setSortOption(sortOption: SortOption) {
        if (_uiState.value.sortOption == sortOption) return
        clearPrefetchCache()
        _uiState.update { it.copy(sortOption = sortOption) }
        loadHome(1)
    }

    fun setHideBlacklisted(value: Boolean) {
        if (_uiState.value.hideBlacklisted == value) return
        clearPrefetchCache()
        _uiState.update { it.copy(hideBlacklisted = value) }
        loadHome(1)
    }

    /**
     * Apply all settings-derived filters at once, triggering at most one loadHome(1).
     * Use this instead of calling setLanguageFilter/setSortOption/setHideBlacklisted
     * separately when applying initial/restored settings, to avoid N concurrent loads.
     */
    fun applySettingsFilters(
        languageFilter: HomeLanguageFilter,
        sortOption: SortOption,
        hideBlacklisted: Boolean,
        loadIfChanged: Boolean = true
    ) {
        val current = _uiState.value
        val filterChanged = current.languageFilter != languageFilter
        val sortChanged = current.sortOption != sortOption
        val hideChanged = current.hideBlacklisted != hideBlacklisted

        if (!filterChanged && !sortChanged && !hideChanged) return
        clearPrefetchCache()

        _uiState.update {
            it.copy(
                languageFilter = languageFilter,
                sortOption = sortOption,
                hideBlacklisted = hideBlacklisted
            )
        }
        if (loadIfChanged) loadHome(1)
    }

    fun loadHome(page: Int = 1) {
        val filter = _uiState.value.languageFilter
        val sort = _uiState.value.sortOption
        val hideBlacklisted = _uiState.value.hideBlacklisted
        val requestKey = HomeRequestKey(filter = filter, sort = sort, hideBlacklisted = hideBlacklisted)
        _uiState.update { it.copy(galleryListState = LoadState.Loading, currentPage = page) }
        viewModelScope.launch {
            val cachedPage = if (
                prefetchedRequestKey == requestKey &&
                prefetchedNextPageSourcePage == (page - 1)
            ) {
                prefetchedNextPage?.takeIf { it.page == page }
            } else {
                null
            }
            val result = if (cachedPage != null) {
                Result.success(cachedPage)
            } else {
                fetchPage(filter = filter, sort = sort, page = page)
            }

            result.fold(
                onSuccess = { pageData ->
                    val filteredItems = applyBlacklistFilter(pageData.items, hideBlacklisted)
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
                    prefetchNextPageIfNeeded(
                        sourcePage = pageData.page,
                        totalPages = pageData.totalPages.coerceAtLeast(1),
                        key = requestKey
                    )
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(galleryListState = LoadState.Error(throwable.message ?: "Unknown error"))
                    }
                }
            )
        }
    }

    fun addToLocalFavorites(item: GallerySummary) {
        viewModelScope.launch {
            libraryRepository.addFavorite(item)
        }
    }

    fun persistHomeSortOption(sortOption: SortOption) {
        viewModelScope.launch {
            val value = when (sortOption) {
                SortOption.POPULAR -> "popular"
                SortOption.RECENT -> "recent"
                SortOption.RANDOM -> "random"
            }
            settingsRepository.setHomeSortOption(value)
        }
    }

    fun persistHomeLanguageFilter(filter: HomeLanguageFilter) {
        viewModelScope.launch {
            val value = when (filter) {
                HomeLanguageFilter.ALL -> "all"
                HomeLanguageFilter.JAPANESE -> "japanese"
                HomeLanguageFilter.CHINESE -> "chinese"
            }
            settingsRepository.setHomeLanguageFilter(value)
        }
    }

    fun persistHideBlacklisted(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideBlacklisted(value)
        }
    }

    private fun Page<GallerySummary>.shuffleItems(): Page<GallerySummary> {
        return copy(items = items.shuffled())
    }

    private suspend fun fetchPage(
        filter: HomeLanguageFilter,
        sort: SortOption,
        page: Int
    ): Result<Page<GallerySummary>> {
        return when (filter) {
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
    }

    private fun applyBlacklistFilter(
        items: List<GallerySummary>,
        hideBlacklisted: Boolean
    ): List<GallerySummary> {
        return if (hideBlacklisted) {
            items.filterNot { it.blacklisted }
        } else {
            items
        }
    }

    private fun prefetchNextPageIfNeeded(
        sourcePage: Int,
        totalPages: Int,
        key: HomeRequestKey
    ) {
        val nextPage = sourcePage + 1
        if (nextPage > totalPages) {
            clearPrefetchCache()
            return
        }
        if (prefetchedRequestKey == key &&
            prefetchedNextPageSourcePage == sourcePage &&
            prefetchedNextPage?.page == nextPage
        ) {
            return
        }

        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val result = fetchPage(filter = key.filter, sort = key.sort, page = nextPage)
            result.onSuccess { pageData ->
                prefetchedRequestKey = key
                prefetchedNextPageSourcePage = sourcePage
                prefetchedNextPage = pageData
            }
        }
    }

    private fun clearPrefetchCache() {
        prefetchJob?.cancel()
        prefetchJob = null
        prefetchedNextPage = null
        prefetchedNextPageSourcePage = null
        prefetchedRequestKey = null
    }

    private data class HomeRequestKey(
        val filter: HomeLanguageFilter,
        val sort: SortOption,
        val hideBlacklisted: Boolean
    )

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
