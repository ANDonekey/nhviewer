package com.nhviewer.data.remote

import com.nhviewer.core.network.ApiResult
import com.nhviewer.data.remote.dto.GalleryDetailDto
import com.nhviewer.data.remote.dto.GalleryCommentDto
import com.nhviewer.data.remote.dto.FavoriteActionDto
import com.nhviewer.data.remote.dto.GalleryListDto
import com.nhviewer.data.remote.dto.TagDto
import com.nhviewer.data.remote.dto.TagSearchRequestDto
import com.nhviewer.data.remote.dto.UserMeDto
import com.nhviewer.data.remote.dto.UserProfileDto
import java.io.IOException
import retrofit2.HttpException

class NhentaiRemoteDataSource(
    private val service: NhentaiService
) {
    suspend fun getAll(page: Int, perPage: Int = 25): ApiResult<GalleryListDto> = safeApiCall {
        service.getAllGalleries(page = page, perPage = perPage).copy(page = page)
    }

    suspend fun getPopular(page: Int): ApiResult<GalleryListDto> = safeApiCall {
        GalleryListDto(
            result = service.getPopular(page),
            page = page,
            numPages = page
        )
    }

    suspend fun search(query: String, sort: String?, page: Int): ApiResult<GalleryListDto> = safeApiCall {
        service.searchGalleries(query = query, sort = sort, page = page)
    }

    suspend fun getTagged(tagId: Long, sort: String?, page: Int, perPage: Int): ApiResult<GalleryListDto> = safeApiCall {
        service.getTaggedGalleries(tagId = tagId, sort = sort, page = page, perPage = perPage)
    }

    suspend fun getDetail(galleryId: Long): ApiResult<GalleryDetailDto> = safeApiCall {
        service.getGalleryDetail(galleryId = galleryId)
    }

    suspend fun getComments(galleryId: Long): ApiResult<List<GalleryCommentDto>> = safeApiCall {
        service.getGalleryComments(galleryId = galleryId)
    }

    suspend fun getFavorites(page: Int): ApiResult<GalleryListDto> = safeApiCall {
        service.getFavorites(page = page).copy(page = page)
    }

    suspend fun checkFavorite(galleryId: Long): ApiResult<FavoriteActionDto> = safeApiCall {
        service.checkFavorite(galleryId = galleryId)
    }

    suspend fun addFavorite(galleryId: Long): ApiResult<FavoriteActionDto> = safeApiCall {
        service.addFavorite(galleryId = galleryId)
    }

    suspend fun removeFavorite(galleryId: Long): ApiResult<FavoriteActionDto> = safeApiCall {
        service.removeFavorite(galleryId = galleryId)
    }

    suspend fun getMe(): ApiResult<UserMeDto> = safeApiCall {
        service.getMe()
    }

    suspend fun getUserProfile(userId: Long, slug: String): ApiResult<UserProfileDto> = safeApiCall {
        service.getUserProfile(userId = userId, slug = slug)
    }

    suspend fun searchTags(query: String): ApiResult<List<TagDto>> = safeApiCall {
        service.searchTags(
            TagSearchRequestDto(
                type = "tag",
                query = query,
                limit = 15
            )
        )
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (exception: HttpException) {
            ApiResult.HttpError(exception.code(), exception.message())
        } catch (exception: IOException) {
            ApiResult.NetworkError(exception)
        } catch (throwable: Throwable) {
            ApiResult.UnknownError(throwable)
        }
    }
}
