package com.nhviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val galleryId: Long,
    val title: String,
    val coverUrl: String?,
    val pageCount: Int,
    val savedAt: Long
)
