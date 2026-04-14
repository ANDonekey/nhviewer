package com.nhviewer.ui.detail

import com.nhviewer.domain.model.AppSettings
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.PageImage
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.repository.GalleryRepository
import com.nhviewer.domain.repository.LibraryRepository
import com.nhviewer.domain.repository.ReaderProgressRepository
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.domain.usecase.GetGalleryCommentsUseCase
import com.nhviewer.domain.usecase.GetGalleryDetailUseCase
import com.nhviewer.testutil.MainDispatcherRule
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loadDetail success updates detail and comments`() = runTest {
        val galleryRepo = FakeGalleryRepository()
        val libraryRepo = FakeLibraryRepository()
        val readerRepo = FakeReaderProgressRepository()
        val settingsRepo = FakeSettingsRepository(AppSettings(favoritesSource = "local"))
        val vm = buildVm(galleryRepo, libraryRepo, readerRepo, settingsRepo)

        vm.loadDetail(55)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.detailState is LoadState.Content)
        assertTrue(vm.uiState.value.commentsState is LoadState.Content)
        assertEquals(3, vm.uiState.value.savedProgress)
        assertEquals(1, libraryRepo.upsertHistoryCount)
    }

    @Test
    fun `toggleFavorite local mode adds local favorite`() = runTest {
        val galleryRepo = FakeGalleryRepository()
        val libraryRepo = FakeLibraryRepository()
        val readerRepo = FakeReaderProgressRepository()
        val settingsRepo = FakeSettingsRepository(AppSettings(favoritesSource = "local"))
        val vm = buildVm(galleryRepo, libraryRepo, readerRepo, settingsRepo)

        vm.loadDetail(55)
        advanceUntilIdle()
        vm.toggleFavorite()
        advanceUntilIdle()

        assertEquals(1, libraryRepo.addFavoriteCount)
        assertEquals(0, galleryRepo.addOnlineCount)
    }

    @Test
    fun `toggleFavorite online mode calls remove when already favorite`() = runTest {
        val galleryRepo = FakeGalleryRepository().apply {
            checkFavoriteResult = true
            removeFavoriteOnlineResult = false
        }
        val libraryRepo = FakeLibraryRepository()
        val readerRepo = FakeReaderProgressRepository()
        val settingsRepo = FakeSettingsRepository(AppSettings(favoritesSource = "online"))
        val vm = buildVm(galleryRepo, libraryRepo, readerRepo, settingsRepo)

        vm.loadDetail(55)
        advanceUntilIdle()
        vm.toggleFavorite()
        advanceUntilIdle()

        assertEquals(1, galleryRepo.removeOnlineCount)
        assertEquals(0, libraryRepo.addFavoriteCount)
        assertEquals(false, vm.uiState.value.isFavorite)
    }

    private fun buildVm(
        galleryRepo: FakeGalleryRepository,
        libraryRepo: FakeLibraryRepository,
        readerRepo: FakeReaderProgressRepository,
        settingsRepo: FakeSettingsRepository
    ): DetailViewModel {
        return DetailViewModel(
            getGalleryDetailUseCase = GetGalleryDetailUseCase(galleryRepo),
            getGalleryCommentsUseCase = GetGalleryCommentsUseCase(galleryRepo),
            galleryRepository = galleryRepo,
            libraryRepository = libraryRepo,
            readerProgressRepository = readerRepo,
            settingsRepository = settingsRepo
        )
    }

    private class FakeGalleryRepository : GalleryRepository {
        var addOnlineCount = 0
        var removeOnlineCount = 0
        var checkFavoriteResult = false
        var removeFavoriteOnlineResult = false

        override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> {
            return Result.success(
                GalleryDetail(
                    id = galleryId,
                    title = "detail",
                    coverUrl = null,
                    pageCount = 10,
                    images = listOf(PageImage(1, "url", null)),
                    tags = emptyList()
                )
            )
        }

        override suspend fun getComments(galleryId: Long): Result<List<GalleryComment>> {
            return Result.success(
                listOf(
                    GalleryComment(1, galleryId, "u", "body", 0L)
                )
            )
        }

        override suspend fun checkFavorite(galleryId: Long): Result<Boolean> = Result.success(checkFavoriteResult)

        override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> {
            addOnlineCount += 1
            return Result.success(true)
        }

        override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> {
            removeOnlineCount += 1
            return Result.success(removeFavoriteOnlineResult)
        }

        override suspend fun getAll(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun search(
            query: String,
            page: Int,
            sort: SortOption,
            tags: List<Tag>
        ): Result<Page<GallerySummary>> = Result.failure(notUsed())

        override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getMe(): Result<UserMe> = Result.failure(notUsed())
        override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> = Result.failure(notUsed())
        override suspend fun searchTags(keyword: String): Result<List<Tag>> = Result.failure(notUsed())

        private fun notUsed(): Throwable = IllegalStateException("not used")
    }

    private class FakeLibraryRepository : LibraryRepository {
        val isFavoriteFlow = MutableStateFlow(false)
        var addFavoriteCount = 0
        var removeFavoriteCount = 0
        var upsertHistoryCount = 0

        override fun observeFavorites(): Flow<List<GallerySummary>> = flowOf(emptyList())

        override fun observeIsFavorite(galleryId: Long): Flow<Boolean> = isFavoriteFlow

        override suspend fun addFavorite(item: GallerySummary) {
            addFavoriteCount += 1
            isFavoriteFlow.value = true
        }

        override suspend fun removeFavorite(galleryId: Long) {
            removeFavoriteCount += 1
            isFavoriteFlow.value = false
        }

        override fun observeHistory(): Flow<List<GallerySummary>> = flowOf(emptyList())

        override suspend fun upsertHistory(item: GallerySummary, lastReadPage: Int) {
            upsertHistoryCount += 1
        }

        override suspend fun removeHistory(galleryId: Long) = Unit
        override suspend fun clearHistory() = Unit
    }

    private class FakeReaderProgressRepository : ReaderProgressRepository {
        override fun observeProgress(galleryId: Long): Flow<Int?> = flowOf(3)

        override suspend fun saveProgress(galleryId: Long, pageIndex: Int) = Unit
    }

    private class FakeSettingsRepository(
        private val settings: AppSettings
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = flowOf(settings)
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


