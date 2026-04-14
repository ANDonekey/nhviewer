package com.nhviewer.ui.library

import com.nhviewer.domain.model.AppSettings
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `local source collects local favorites`() = runTest {
        val library = FakeLibraryRepository().apply {
            favoritesFlow.value = listOf(gallery(1), gallery(2))
        }
        val galleryRepo = FakeGalleryRepository()
        val settings = FakeSettingsRepository(
            AppSettings(favoritesSource = "local", hideBlacklisted = false)
        )

        val vm = FavoritesViewModel(library, galleryRepo, settings)
        advanceUntilIdle()

        assertEquals("local", vm.uiState.value.source)
        assertEquals(2, vm.uiState.value.list.size)
        assertEquals(0, galleryRepo.getFavoritesCalls)
    }

    @Test
    fun `online source loads online favorites and applies hide blacklisted`() = runTest {
        val library = FakeLibraryRepository()
        val galleryRepo = FakeGalleryRepository().apply {
            favoritesPage = Page(
                items = listOf(
                    gallery(1, blacklisted = false),
                    gallery(2, blacklisted = true)
                ),
                page = 1,
                totalPages = 1
            )
        }
        val settings = FakeSettingsRepository(
            AppSettings(favoritesSource = "online", hideBlacklisted = true)
        )

        val vm = FavoritesViewModel(library, galleryRepo, settings)
        advanceUntilIdle()

        assertEquals("online", vm.uiState.value.source)
        assertEquals(1, vm.uiState.value.list.size)
        assertEquals(1L, vm.uiState.value.list.first().id)
        assertEquals(1, galleryRepo.getFavoritesCalls)
    }

    @Test
    fun `switching source from local to online reloads online list`() = runTest {
        val library = FakeLibraryRepository().apply {
            favoritesFlow.value = listOf(gallery(11))
        }
        val galleryRepo = FakeGalleryRepository().apply {
            favoritesPage = Page(items = listOf(gallery(22)), page = 1, totalPages = 1)
        }
        val settings = FakeSettingsRepository(
            AppSettings(favoritesSource = "local")
        )

        val vm = FavoritesViewModel(library, galleryRepo, settings)
        advanceUntilIdle()
        assertEquals(11L, vm.uiState.value.list.first().id)

        settings.emit(AppSettings(favoritesSource = "online"))
        advanceUntilIdle()

        assertEquals("online", vm.uiState.value.source)
        assertEquals(22L, vm.uiState.value.list.first().id)
        assertEquals(1, galleryRepo.getFavoritesCalls)
    }

    private fun gallery(id: Long, blacklisted: Boolean = false): GallerySummary {
        return GallerySummary(
            id = id,
            title = "g$id",
            coverUrl = null,
            pageCount = 10,
            tags = emptyList(),
            blacklisted = blacklisted
        )
    }

    private class FakeLibraryRepository : LibraryRepository {
        val favoritesFlow = MutableStateFlow<List<GallerySummary>>(emptyList())

        override fun observeFavorites(): Flow<List<GallerySummary>> = favoritesFlow
        override fun observeIsFavorite(galleryId: Long): Flow<Boolean> = flowOf(false)
        override suspend fun addFavorite(item: GallerySummary) = Unit
        override suspend fun removeFavorite(galleryId: Long) = Unit
        override fun observeHistory(): Flow<List<GallerySummary>> = flowOf(emptyList())
        override suspend fun upsertHistory(item: GallerySummary, lastReadPage: Int) = Unit
        override suspend fun removeHistory(galleryId: Long) = Unit
        override suspend fun clearHistory() = Unit
    }

    private class FakeGalleryRepository : GalleryRepository {
        var favoritesPage: Page<GallerySummary> = Page(emptyList(), 1, 1)
        var getFavoritesCalls: Int = 0

        override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> {
            getFavoritesCalls += 1
            return Result.success(favoritesPage)
        }

        override suspend fun getAll(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun search(
            query: String,
            page: Int,
            sort: SortOption,
            tags: List<Tag>
        ): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> = Result.failure(notUsed())
        override suspend fun getComments(galleryId: Long): Result<List<GalleryComment>> = Result.failure(notUsed())
        override suspend fun checkFavorite(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun getMe(): Result<UserMe> = Result.failure(notUsed())
        override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> = Result.failure(notUsed())
        override suspend fun searchTags(keyword: String): Result<List<Tag>> = Result.failure(notUsed())

        private fun notUsed(): Throwable = IllegalStateException("not used")
    }

    private class FakeSettingsRepository(
        initial: AppSettings
    ) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        fun emit(value: AppSettings) {
            state.value = value
        }

        override fun observeSettings(): Flow<AppSettings> = state
        override suspend fun setImageQuality(value: String) = Unit
        override suspend fun setMaxConcurrency(value: Int) = Unit
        override suspend fun setThemeMode(value: String) = Unit
        override suspend fun setLanguage(value: String) = Unit
        override suspend fun setPreferJapaneseTitle(value: Boolean) = Unit
        override suspend fun setShowChineseTags(value: Boolean) = Unit
        override suspend fun setHomeLanguageFilter(value: String) = Unit
        override suspend fun setHomeSortOption(value: String) = Unit
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
