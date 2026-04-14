package com.nhviewer.domain.repository

import com.nhviewer.domain.model.GallerySummary
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeFavorites(): Flow<List<GallerySummary>>
    fun observeIsFavorite(galleryId: Long): Flow<Boolean>
    suspend fun addFavorite(item: GallerySummary)
    suspend fun removeFavorite(galleryId: Long)

    fun observeHistory(): Flow<List<GallerySummary>>
    suspend fun upsertHistory(item: GallerySummary, lastReadPage: Int)
    suspend fun removeHistory(galleryId: Long)
    suspend fun clearHistory()
}
