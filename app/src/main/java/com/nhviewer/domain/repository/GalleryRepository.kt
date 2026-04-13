package com.nhviewer.domain.repository

import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile

interface GalleryRepository {
    suspend fun getAll(page: Int): Result<Page<GallerySummary>>
    suspend fun getPopular(page: Int): Result<Page<GallerySummary>>

    suspend fun search(
        query: String,
        page: Int,
        sort: SortOption,
        tags: List<Tag>
    ): Result<Page<GallerySummary>>

    suspend fun getDetail(galleryId: Long): Result<GalleryDetail>
    suspend fun getComments(galleryId: Long): Result<List<GalleryComment>>
    suspend fun getFavorites(page: Int): Result<Page<GallerySummary>>
    suspend fun checkFavorite(galleryId: Long): Result<Boolean>
    suspend fun addFavoriteOnline(galleryId: Long): Result<Boolean>
    suspend fun removeFavoriteOnline(galleryId: Long): Result<Boolean>
    suspend fun getMe(): Result<UserMe>
    suspend fun getUserProfile(userId: Long, slug: String): Result<UserProfile>

    suspend fun searchTags(keyword: String): Result<List<Tag>>
}
