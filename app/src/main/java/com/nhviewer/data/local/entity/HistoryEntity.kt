package com.nhviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val galleryId: Long,
    val title: String,
    val coverUrl: String?,
    val pageCount: Int,
    val lastReadPage: Int,
    val updatedAt: Long
)
