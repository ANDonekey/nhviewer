package com.nhviewer.domain.repository

import kotlinx.coroutines.flow.Flow

interface ReaderProgressRepository {
    fun observeProgress(galleryId: Long): Flow<Int?>
    suspend fun saveProgress(galleryId: Long, pageIndex: Int)
}
