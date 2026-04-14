package com.nhviewer.data.repository

import com.nhviewer.core.network.ApiResult
import com.nhviewer.data.local.dao.TagDao
import com.nhviewer.data.local.mapper.toDomain as toDomainTagEntity
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
    private val remoteDataSource: NhentaiRemoteDataSource,
    private val tagDao: TagDao? = null
) : GalleryRepository {
    private val tagCache = mutableMapOf<Long, Tag>()

    override suspend fun getAll(page: Int): Result<Page<GallerySummary>> {
        return when (val result = remoteDataSource.getAll(page)) {
            is ApiResult.Success -> Result.success(enrichTags(result.data.toSummaryPage()))
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${result.code}: ${result.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(result.exception)
            is ApiResult.UnknownError -> Result.failure(result.throwable)
        }
    }

    override suspend fun getPopular(page: Int): Result<Page<GallerySummary>> {
        return when (val result = remoteDataSource.getPopular(page)) {
            is ApiResult.Success -> Result.success(enrichTags(result.data.toSummaryPage()))
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
            is ApiResult.Success -> Result.success(enrichTags(searchResult.data.toSummaryPage()))
            is ApiResult.HttpError -> Result.failure(RuntimeException("HTTP ${searchResult.code}: ${searchResult.message.orEmpty()}"))
            is ApiResult.NetworkError -> Result.failure(searchResult.exception)
            is ApiResult.UnknownError -> Result.failure(searchResult.throwable)
        }
    }

    override suspend fun getDetail(galleryId: Long): Result<GalleryDetail> {
        return when (val result = remoteDataSource.getDetail(galleryId)) {
            is ApiResult.Success -> Result.success(enrichDetailTags(result.data.toDomainDetail()))
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
            is ApiResult.Success -> Result.success(enrichTags(result.data.toSummaryPage()))
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
        val local = queryLocalTags(keyword)
        if (local.isNotEmpty()) {
            return Result.success(local)
        }

        return Result.success(emptyList())
    }

    private suspend fun enrichTags(page: Page<GallerySummary>): Page<GallerySummary> {
        val requestedIds = page.items
            .flatMap { it.tagIds }
            .distinct()
        if (requestedIds.isEmpty()) return page

        // Always merge local tag catalog first so zh translation updates can refresh cache
        // even after remote tags were cached earlier in this process.
        val localTagsById = tagDao?.getByIds(requestedIds)
            .orEmpty()
            .map { it.toDomainTagEntity() }
            .associateBy { it.id }

        localTagsById.values.forEach { localTag ->
            val cached = tagCache[localTag.id]
            tagCache[localTag.id] = when {
                cached == null -> localTag
                cached.nameZh.isNullOrBlank() && !localTag.nameZh.isNullOrBlank() -> {
                    cached.copy(nameZh = localTag.nameZh)
                }
                else -> cached
            }
        }

        val missingIds = requestedIds.filterNot { tagCache.containsKey(it) }

        if (missingIds.isNotEmpty()) {
            missingIds.chunked(100).forEach { chunk ->
                when (val result = remoteDataSource.getTagsByIds(chunk)) {
                    is ApiResult.Success -> {
                        result.data.map { it.toDomain() }.forEach { remoteTag ->
                            val local = localTagsById[remoteTag.id]
                            tagCache[remoteTag.id] = if (local?.nameZh.isNullOrBlank()) {
                                remoteTag
                            } else {
                                remoteTag.copy(nameZh = local?.nameZh)
                            }
                        }
                    }
                    is ApiResult.HttpError -> Unit
                    is ApiResult.NetworkError -> Unit
                    is ApiResult.UnknownError -> Unit
                }
            }
        }

        val enrichedItems = page.items.map { item ->
            val tags = item.tagIds.mapNotNull { tagCache[it] }
            item.copy(tags = tags)
        }
        return page.copy(items = enrichedItems)
    }

    private suspend fun queryLocalTags(keyword: String): List<Tag> {
        val dao = tagDao ?: return emptyList()
        return dao.searchByKeyword(keyword.trim(), limit = 50).map { it.toDomainTagEntity() }
    }

    private suspend fun enrichDetailTags(detail: GalleryDetail): GalleryDetail {
        if (detail.tags.isEmpty()) return detail

        val ids = detail.tags.map { it.id }.filter { it > 0L }.distinct()
        if (ids.isEmpty()) return detail

        val localById = tagDao?.getByIds(ids)
            .orEmpty()
            .map { it.toDomainTagEntity() }
            .associateBy { it.id }

        if (localById.isEmpty()) return detail

        val merged = detail.tags.map { tag ->
            val local = localById[tag.id]
            if (local?.nameZh.isNullOrBlank()) tag else tag.copy(nameZh = local?.nameZh)
        }
        return detail.copy(tags = merged)
    }
}
