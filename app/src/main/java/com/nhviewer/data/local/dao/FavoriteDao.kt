package com.nhviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhviewer.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE galleryId = :galleryId)")
    fun observeIsFavorite(galleryId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE galleryId = :galleryId")
    suspend fun deleteByGalleryId(galleryId: Long)
}
