package com.nhviewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhviewer.data.local.dao.FavoriteDao
import com.nhviewer.data.local.dao.HistoryDao
import com.nhviewer.data.local.dao.ReadingProgressDao
import com.nhviewer.data.local.dao.TagDao
import com.nhviewer.data.local.entity.FavoriteEntity
import com.nhviewer.data.local.entity.HistoryEntity
import com.nhviewer.data.local.entity.ReadingProgressEntity
import com.nhviewer.data.local.entity.TagEntity

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, ReadingProgressEntity::class, TagEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `slug` TEXT NOT NULL,
                        `name_zh` TEXT,
                        `count` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_type` ON `tags` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_slug` ON `tags` (`slug`)")
            }
        }
    }
}
