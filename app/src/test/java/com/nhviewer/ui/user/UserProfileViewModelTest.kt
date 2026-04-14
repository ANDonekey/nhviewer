package com.nhviewer.ui.user

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
class UserProfileViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `load success returns profile content`() = runTest {
        val repo = FakeGalleryRepository().apply {
            meResult = Result.success(UserMe(id = 1, username = "u", slug = "slug", avatarUrl = "a"))
            profileResult = Result.success(
                UserProfile(
                    id = 1,
                    username = "u",
                    slug = "slug",
                    avatarUrl = "a",
                    about = "",
                    favoriteTags = "",
                    dateJoined = 0L,
                    recentFavorites = emptyList(),
                    recentComments = emptyList()
                )
            )
        }
        val vm = UserProfileViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.profileState is LoadState.Content)
    }

    @Test
    fun `load failure maps 401 message`() = runTest {
        val repo = FakeGalleryRepository().apply {
            meResult = Result.failure(RuntimeException("HTTP 401: Unauthorized"))
        }
        val vm = UserProfileViewModel(repo)

        vm.load()
        advanceUntilIdle()

        val state = vm.uiState.value.profileState
        assertTrue(state is LoadState.Error)
        assertEquals(
            "Unauthorized (401): check API Key in settings.",
            (state as LoadState.Error).message
        )
    }

    private class FakeGalleryRepository : GalleryRepository {
        var meResult: Result<UserMe> = Result.failure(IllegalStateException("not set"))
        var profileResult: Result<UserProfile> = Result.failure(IllegalStateException("not set"))

        override suspend fun getMe(): Result<UserMe> = meResult
        override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> = profileResult

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
        override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> = Result.failure(notUsed())
        override suspend fun checkFavorite(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> = Result.failure(notUsed())
        override suspend fun searchTags(keyword: String): Result<List<Tag>> = Result.failure(notUsed())

        private fun notUsed(): Throwable = IllegalStateException("not used")
    }
}
