package com.nhviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    val galleryId: Long,
    val pageIndex: Int,
    val updatedAt: Long
)
