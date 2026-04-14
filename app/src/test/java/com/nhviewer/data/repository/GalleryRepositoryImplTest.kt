package com.nhviewer.data.repository

import com.nhviewer.data.remote.NhentaiRemoteDataSource
import com.nhviewer.data.remote.NhentaiService
import com.nhviewer.data.remote.dto.FavoriteActionDto
import com.nhviewer.data.remote.dto.GalleryCommentDto
import com.nhviewer.data.remote.dto.GalleryDetailDto
import com.nhviewer.data.remote.dto.GalleryDto
import com.nhviewer.data.remote.dto.GalleryListDto
import com.nhviewer.data.remote.dto.TagDto
import com.nhviewer.data.remote.dto.TagSearchRequestDto
import com.nhviewer.data.remote.dto.UserMeDto
import com.nhviewer.data.remote.dto.UserProfileDto
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryRepositoryImplTest {

    @Test
    fun `getAll maps http error to failure message`() = runTest {
        val service = FakeNhentaiService().apply {
            allGalleriesBlock = { _, _ -> throw httpException(401) }
        }
        val repo = GalleryRepositoryImpl(NhentaiRemoteDataSource(service))

        val result = repo.getAll(page = 1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.startsWith("HTTP 401") == true)
    }

    @Test
    fun `getAll maps network io error`() = runTest {
        val service = FakeNhentaiService().apply {
            allGalleriesBlock = { _, _ -> throw IOException("network down") }
        }
        val repo = GalleryRepositoryImpl(NhentaiRemoteDataSource(service))

        val result = repo.getAll(page = 1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `getAll enriches tags by ids`() = runTest {
        val service = FakeNhentaiService().apply {
            allGalleriesBlock = { page, _ ->
                GalleryListDto(
                    result = listOf(
                        GalleryDto(
                            id = 123,
                            englishTitle = "A",
                            thumbnail = "a.jpg",
                            numPages = 25,
                            tagIds = listOf(10, 11)
                        )
                    ),
                    page = page,
                    numPages = 2
                )
            }
            tagsByIdsBlock = { ids ->
                val idSet = ids.split(",").mapNotNull { it.toLongOrNull() }.toSet()
                listOf(
                    TagDto(id = 10, type = "language", name = "japanese", slug = "japanese"),
                    TagDto(id = 11, type = "category", name = "doujinshi", slug = "doujinshi")
                ).filter { idSet.contains(it.id) }
            }
        }
        val repo = GalleryRepositoryImpl(NhentaiRemoteDataSource(service))

        val result = repo.getAll(page = 1)

        assertTrue(result.isSuccess)
        val item = result.getOrThrow().items.first()
        assertEquals(2, item.tags.size)
        assertEquals("language", item.tags.first().type)
        assertEquals("japanese", item.tags.first().name)
    }

    private fun httpException(code: Int): HttpException {
        val response = Response.error<Any>(
            code,
            """{"error":"unauthorized"}""".toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }

    private class FakeNhentaiService : NhentaiService {
        var allGalleriesBlock: suspend (Int, Int) -> GalleryListDto = { page, _ ->
            GalleryListDto(page = page, numPages = 1)
        }
        var tagsByIdsBlock: suspend (String) -> List<TagDto> = { emptyList() }

        override suspend fun getAllGalleries(page: Int, perPage: Int): GalleryListDto {
            return allGalleriesBlock(page, perPage)
        }

        override suspend fun getPopular(page: Int?): List<GalleryDto> = emptyList()

        override suspend fun searchGalleries(query: String, sort: String?, page: Int): GalleryListDto {
            return GalleryListDto(page = page, numPages = 1)
        }

        override suspend fun getTaggedGalleries(tagId: Long, sort: String?, page: Int, perPage: Int): GalleryListDto {
            return GalleryListDto(page = page, numPages = 1)
        }

        override suspend fun getGalleryDetail(galleryId: Long, include: String?): GalleryDetailDto {
            throw UnsupportedOperationException("not needed")
        }

        override suspend fun getGalleryComments(galleryId: Long): List<GalleryCommentDto> = emptyList()

        override suspend fun getFavorites(page: Int): GalleryListDto = GalleryListDto(page = page, numPages = 1)

        override suspend fun checkFavorite(galleryId: Long): FavoriteActionDto = FavoriteActionDto(false)

        override suspend fun addFavorite(galleryId: Long): FavoriteActionDto = FavoriteActionDto(true)

        override suspend fun removeFavorite(galleryId: Long): FavoriteActionDto = FavoriteActionDto(false)

        override suspend fun getMe(): UserMeDto = UserMeDto()

        override suspend fun getUserProfile(userId: Long, slug: String): UserProfileDto = UserProfileDto()

        override suspend fun searchTags(request: TagSearchRequestDto): List<TagDto> = emptyList()

        override suspend fun getTagsByIds(ids: String): List<TagDto> = tagsByIdsBlock(ids)
    }
}
