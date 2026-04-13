package com.nhviewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nhviewer.data.local.dao.FavoriteDao
import com.nhviewer.data.local.dao.HistoryDao
import com.nhviewer.data.local.dao.ReadingProgressDao
import com.nhviewer.data.local.entity.FavoriteEntity
import com.nhviewer.data.local.entity.HistoryEntity
import com.nhviewer.data.local.entity.ReadingProgressEntity

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, ReadingProgressEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingProgressDao(): ReadingProgressDao
}
