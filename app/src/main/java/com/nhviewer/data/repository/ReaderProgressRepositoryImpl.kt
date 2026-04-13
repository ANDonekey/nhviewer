package com.nhviewer.data.repository

import com.nhviewer.data.local.dao.ReadingProgressDao
import com.nhviewer.data.local.entity.ReadingProgressEntity
import com.nhviewer.domain.repository.ReaderProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderProgressRepositoryImpl(
    private val dao: ReadingProgressDao
) : ReaderProgressRepository {
    override fun observeProgress(galleryId: Long): Flow<Int?> {
        return dao.observeByGalleryId(galleryId).map { it?.pageIndex }
    }

    override suspend fun saveProgress(galleryId: Long, pageIndex: Int) {
        dao.upsert(
            ReadingProgressEntity(
                galleryId = galleryId,
                pageIndex = pageIndex,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
