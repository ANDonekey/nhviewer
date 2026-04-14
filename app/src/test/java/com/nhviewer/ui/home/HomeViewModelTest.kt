package com.nhviewer.ui.home

import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.model.UserRecentComment
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetAllGalleriesUseCase
import com.nhviewer.domain.usecase.GetPopularGalleriesUseCase
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.testutil.MainDispatcherRule
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadHome hides blacklisted when setting enabled`() = runTest {
        val repo = FakeGalleryRepository().apply {
            allPage = Page(
                items = listOf(
                    gallery(id = 1, blacklisted = false),
                    gallery(id = 2, blacklisted = true)
                ),
                page = 1,
                totalPages = 2
            )
        }
        val vm = buildViewModel(repo)

        vm.setHideBlacklisted(true)
        advanceUntilIdle()

        val state = vm.uiState.value.galleryListState
        assertTrue(state is LoadState.Content)
        val list = (state as LoadState.Content<List<GallerySummary>>).value
        assertEquals(1, list.size)
        assertEquals(1L, list.first().id)
    }

    @Test
    fun `loadHome japanese filter uses language tag and empty query`() = runTest {
        val languageTag = Tag(id = 99, type = "language", name = "japanese", slug = "japanese")
        val repo = FakeGalleryRepository().apply {
            tagSearchResult = listOf(languageTag)
            searchPage = Page(items = listOf(gallery(10)), page = 1, totalPages = 1)
        }
        val vm = buildViewModel(repo)

        vm.setLanguageFilter(HomeLanguageFilter.JAPANESE)
        advanceUntilIdle()

        assertEquals("", repo.lastSearchQuery)
        assertEquals(listOf(languageTag), repo.lastSearchTags)
    }

    @Test
    fun `loadHome all popular uses popular usecase`() = runTest {
        val repo = FakeGalleryRepository().apply {
            popularPage = Page(items = listOf(gallery(7)), page = 1, totalPages = 1)
        }
        val vm = buildViewModel(repo)

        vm.setSortOption(SortOption.POPULAR)
        advanceUntilIdle()

        assertEquals(1, repo.popularCallCount)
        val state = vm.uiState.value.galleryListState
        assertTrue(state is LoadState.Content)
    }

    @Test
    fun `addToLocalFavorites delegates to library repository`() = runTest {
        val repo = FakeGalleryRepository()
        val libraryRepo = FakeLibraryRepository()
        val vm = buildViewModel(repo, libraryRepo = libraryRepo)

        vm.addToLocalFavorites(gallery(42))
        advanceUntilIdle()

        assertEquals(1, libraryRepo.addFavoriteCalls)
        assertEquals(42L, libraryRepo.lastAddedId)
    }

    @Test
    fun `persistHomeSortOption writes settings value`() = runTest {
        val repo = FakeGalleryRepository()
        val settingsRepo = FakeSettingsRepository()
        val vm = buildViewModel(repo, settingsRepository = settingsRepo)

        vm.persistHomeSortOption(SortOption.POPULAR)
        advanceUntilIdle()

        assertEquals("popular", settingsRepo.lastSortOption)
    }

    private fun buildViewModel(
        repo: FakeGalleryRepository,
        libraryRepo: FakeLibraryRepository = FakeLibraryRepository(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository()
    ): HomeViewModel {
        return HomeViewModel(
            getAllGalleriesUseCase = GetAllGalleriesUseCase(repo),
            getPopularGalleriesUseCase = GetPopularGalleriesUseCase(repo),
            searchGalleriesUseCase = SearchGalleriesUseCase(repo),
            searchTagsUseCase = SearchTagsUseCase(repo),
            libraryRepository = libraryRepo,
            settingsRepository = settingsRepository
        )
    }

    private fun gallery(id: Long, blacklisted: Boolean = false): GallerySummary {
        return GallerySummary(
            id = id,
            title = "title-$id",
            englishTitle = "en-$id",
            japaneseTitle = "jp-$id",
            coverUrl = null,
            pageCount = 10,
            tags = emptyList(),
            tagIds = emptyList(),
            blacklisted = blacklisted
        )
    }

    private class FakeGalleryRepository : GalleryRepository {
        var allPage: Page<GallerySummary> = Page(emptyList(), 1, 1)
        var popularPage: Page<GallerySummary> = Page(emptyList(), 1, 1)
        var searchPage: Page<GallerySummary> = Page(emptyList(), 1, 1)
        var tagSearchResult: List<Tag> = emptyList()

        var lastSearchQuery: String? = null
        var lastSearchTags: List<Tag> = emptyList()
        var popularCallCount: Int = 0

        override suspend fun getAll(page: Int): Result<Page<GallerySummary>> = Result.success(allPage)

        override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> {
            popularCallCount += 1
            return Result.success(popularPage)
        }

        override suspend fun search(
            query: String,
            page: Int,
            sort: SortOption,
            tags: List<Tag>
        ): Result<Page<GallerySummary>> {
            lastSearchQuery = query
            lastSearchTags = tags
            return Result.success(searchPage)
        }

        override suspend fun searchTags(keyword: String): Result<List<Tag>> = Result.success(tagSearchResult)

        override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> = Result.failure(notUsed())
        override suspend fun getComments(galleryId: Long): Result<List<GalleryComment>> = Result.failure(notUsed())
        override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun checkFavorite(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun getMe(): Result<UserMe> = Result.failure(notUsed())
        override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> = Result.failure(notUsed())

        private fun notUsed(): Throwable = IllegalStateException("not used in this test")
    }

    private class FakeLibraryRepository : LibraryRepository {
        var addFavoriteCalls = 0
        var lastAddedId: Long? = null
        override fun observeFavorites(): Flow<List<GallerySummary>> = flowOf(emptyList())
        override fun observeIsFavorite(galleryId: Long): Flow<Boolean> = flowOf(false)
        override suspend fun addFavorite(item: GallerySummary) {
            addFavoriteCalls += 1
            lastAddedId = item.id
        }
        override suspend fun removeFavorite(galleryId: Long) = Unit
        override fun observeHistory(): Flow<List<GallerySummary>> = flowOf(emptyList())
        override suspend fun upsertHistory(item: GallerySummary, lastReadPage: Int) = Unit
        override suspend fun removeHistory(galleryId: Long) = Unit
        override suspend fun clearHistory() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        var lastSortOption: String? = null
        override fun observeSettings(): Flow<com.nhviewer.domain.model.AppSettings> =
            flowOf(com.nhviewer.domain.model.AppSettings())
        override suspend fun setImageQuality(value: String) = Unit
        override suspend fun setMaxConcurrency(value: Int) = Unit
        override suspend fun setThemeMode(value: String) = Unit
        override suspend fun setLanguage(value: String) = Unit
        override suspend fun setPreferJapaneseTitle(value: Boolean) = Unit
        override suspend fun setShowChineseTags(value: Boolean) = Unit
        override suspend fun setHomeLanguageFilter(value: String) = Unit
        override suspend fun setHomeSortOption(value: String) {
            lastSortOption = value
        }
        override suspend fun setApiKey(value: String) = Unit
        override suspend fun setHideBlacklisted(value: Boolean) = Unit
        override suspend fun setFavoritesSource(value: String) = Unit
        override suspend fun setReaderTapPagingEnabled(value: Boolean) = Unit
        override suspend fun setReaderSwipePagingEnabled(value: Boolean) = Unit
        override suspend fun setReaderTapToToggleChromeEnabled(value: Boolean) = Unit
        override suspend fun setReaderReverseTapZones(value: Boolean) = Unit
        override suspend fun setReaderGestureEnabled(value: Boolean) = Unit
        override suspend fun setReaderLeftHandedMode(value: Boolean) = Unit
        override suspend fun setReaderPagingMode(value: String) = Unit
    }
}
