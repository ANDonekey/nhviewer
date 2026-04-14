package com.nhviewer.data.repository

import com.nhviewer.data.local.dao.FavoriteDao
import com.nhviewer.data.local.dao.HistoryDao
import com.nhviewer.data.local.entity.FavoriteEntity
import com.nhviewer.data.local.entity.HistoryEntity
import com.nhviewer.data.local.mapper.toDomain
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao
) : LibraryRepository {
    override fun observeFavorites(): Flow<List<GallerySummary>> {
        return favoriteDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeIsFavorite(galleryId: Long): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(galleryId)
    }

    override suspend fun addFavorite(item: GallerySummary) {
        favoriteDao.upsert(
            FavoriteEntity(
                galleryId = item.id,
                title = item.title,
                coverUrl = item.coverUrl,
                pageCount = item.pageCount,
                savedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFavorite(galleryId: Long) {
        favoriteDao.deleteByGalleryId(galleryId)
    }

    override fun observeHistory(): Flow<List<GallerySummary>> {
        return historyDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun upsertHistory(item: GallerySummary, lastReadPage: Int) {
        historyDao.upsert(
            HistoryEntity(
                galleryId = item.id,
                title = item.title,
                coverUrl = item.coverUrl,
                pageCount = item.pageCount,
                lastReadPage = lastReadPage,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeHistory(galleryId: Long) {
        historyDao.deleteByGalleryId(galleryId)
    }

    override suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
