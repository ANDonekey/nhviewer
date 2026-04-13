package com.nhviewer.data.repository

import com.nhviewer.core.network.ApiResult
import com.nhviewer.data.mapper.toDomain
import com.nhviewer.data.mapper.toDomainDetail
import com.nhviewer.data.mapper.toSummaryPage
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.data.remote.NhentaiRemoteDataSource
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.repository.GalleryRepository

class GalleryRepositoryImpl(
    private val remoteDataSource: NhentaiRemoteDataSource
) : GalleryRepository {

    override suspend fun getAll(page: Int): Result<Page<GallerySummary>> {
        return when (val result = remoteDataSource.getAll(page)) {
            is ApiResult.Success -> Result.success(result.data.toSummaryPage())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> {
        return when (val result = remoteDataSource.getPopular(page)) {
            is ApiResult.Success -> Result.success(result.data.toSummaryPage())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun search(
        query: String,
        page: Int,
        sort: SortOption,
        tags: List<Tag>
    ): Result<Page<GallerySummary>> {
        val apiSort = when (sort) {
            SortOption.POPULAR -> "popular"
            SortOption.RECENT -> "date"
            SortOption.RANDOM -> "date"
        }

        val searchResult = if (tags.isNotEmpty()) {
            // First-phase skeleton: only the first tag is used for remote tagged search.
            val firstTag = tags.first()
            remoteDataSource.getTagged(tagId = firstTag.id, sort = apiSort, page = page, perPage = 25)
        } else {
            remoteDataSource.search(query = query, sort = apiSort, page = page)
        }

        return when (searchResult) {
            is ApiResult.Success -> Result.success(searchResult.data.toSummaryPage())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${searchResult.code}: ${searchResult.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(searchResult.exception)
            is ApiResult.UnknownError -> Result.failure(searchResult.throwable)
        }
    }

    override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> {
        return when (val result = remoteDataSource.getDetail(galleryId)) {
            is ApiResult.Success -> Result.success(result.data.toDomainDetail())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getComments(galleryId: Long): Result<List<GalleryComment>> {
        return when (val result = remoteDataSource.getComments(galleryId)) {
            is ApiResult.Success -> Result.success(result.data.map { it.toDomain() })
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getFavorites(page: Int): Result<Page<GallerySummary>> {
        return when (val result = remoteDataSource.getFavorites(page)) {
            is ApiResult.Success -> Result.success(result.data.toSummaryPage())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun checkFavorite(galleryId: Long): Result<Boolean> {
        return when (val result = remoteDataSource.checkFavorite(galleryId)) {
            is ApiResult.Success -> Result.success(result.data.favorited)
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean> {
        return when (val result = remoteDataSource.addFavorite(galleryId)) {
            is ApiResult.Success -> Result.success(result.data.favorited)
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean> {
        return when (val result = remoteDataSource.removeFavorite(galleryId)) {
            is ApiResult.Success -> Result.success(result.data.favorited)
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getMe(): Result<UserMe> {
        return when (val result = remoteDataSource.getMe()) {
            is ApiResult.Success -> Result.success(result.data.toDomain())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile> {
        return when (val result = remoteDataSource.getUserProfile(userId = userId, slug = slug)) {
            is ApiResult.Success -> Result.success(result.data.toDomain())
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun searchTags(keyword: String): Result<List<Tag>> {
        return when (val result = remoteDataSource.searchTags(keyword)) {
            is ApiResult.Success -> Result.success(result.data.map { it.toDomain() })
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }
}
