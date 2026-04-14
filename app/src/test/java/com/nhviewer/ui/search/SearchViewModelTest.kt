package com.nhviewer.ui.search

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
import com.nhviewer.domain.usecase.SearchGalleriesUseCase
import com.nhviewer.domain.usecase.SearchTagsUseCase
import com.nhviewer.testutil.MainDispatcherRule
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `search success updates content and pages`() = runTest {
        val repo = FakeGalleryRepository().apply {
            searchResult = Page(items = listOf(gallery(1)), page = 2, totalPages = 9)
        }
        val vm = SearchViewModel(
            searchGalleriesUseCase = SearchGalleriesUseCase(repo),
            searchTagsUseCase = SearchTagsUseCase(repo)
        )

        vm.updateKeyword("test")
        vm.search(page = 2)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.resultState is LoadState.Content)
        assertEquals(2, vm.uiState.value.queryState.page)
        assertEquals(9, vm.uiState.value.totalPages)
    }

    @Test
    fun `addTagByKeyword appends first tag and deduplicates`() = runTest {
        val repo = FakeGalleryRepository().apply {
            tagsResult = listOf(
                Tag(id = 100, type = "language", name = "japanese", slug = "japanese"),
                Tag(id = 101, type = "language", name = "chinese", slug = "chinese")
            )
        }
        val vm = SearchViewModel(
            searchGalleriesUseCase = SearchGalleriesUseCase(repo),
            searchTagsUseCase = SearchTagsUseCase(repo)
        )

        vm.addTagByKeyword("japanese")
        vm.addTagByKeyword("japanese")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.queryState.selectedTags.size)
        assertEquals(100L, vm.uiState.value.queryState.selectedTags.first().id)
    }

    @Test
    fun `search failure updates error state`() = runTest {
        val repo = FakeGalleryRepository().apply {
            searchError = RuntimeException("boom")
        }
        val vm = SearchViewModel(
            searchGalleriesUseCase = SearchGalleriesUseCase(repo),
            searchTagsUseCase = SearchTagsUseCase(repo)
        )

        vm.search(page = 1)
        advanceUntilIdle()

        val state = vm.uiState.value.resultState
        assertTrue(state is LoadState.Error)
        assertEquals("boom", (state as LoadState.Error).message)
    }

    private fun gallery(id: Long): GallerySummary {
        return GallerySummary(
            id = id,
            title = "g$id",
            coverUrl = null,
            pageCount = 10,
            tags = emptyList()
        )
    }

    private class FakeGalleryRepository : GalleryRepository {
        var searchResult: Page<GallerySummary> = Page(emptyList(), 1, 1)
        var searchError: Throwable? = null
        var tagsResult: List<Tag> = emptyList()

        override suspend fun search(
            query: String,
            page: Int,
            sort: SortOption,
            tags: List<Tag>
        ): Result<Page<GallerySummary>> {
            val error = searchError
            return if (error != null) Result.failure(error) else Result.success(searchResult)
        }

        override suspend fun searchTags(keyword: String): Result<List<Tag>> = Result.success(tagsResult)

        override suspend fun getAll(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> = Result.failure(notUsed())
        override suspend fun getComments(galleryId: Long): Result<List<GalleryComment>> = Result.failure(notUsed())
        override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun checkFavorite(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun getMe(): Result<UserMe> = Result.failure(notUsed())
        override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> = Result.failure(notUsed())

        private fun notUsed(): Throwable = IllegalStateException("not used")
    }
}
