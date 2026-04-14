package com.nhviewer.data.local.mapper

import com.nhviewer.data.local.entity.FavoriteEntity
import com.nhviewer.data.local.entity.HistoryEntity
import com.nhviewer.data.local.entity.TagEntity
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Tag

fun FavoriteEntity.toDomain(): GallerySummary = GallerySummary(
    id = galleryId,
    title = title,
    englishTitle = null,
    japaneseTitle = null,
    coverUrl = coverUrl,
    pageCount = pageCount,
    tags = emptyList()
)

fun HistoryEntity.toDomain(): GallerySummary = GallerySummary(
    id = galleryId,
    title = title,
    englishTitle = null,
    japaneseTitle = null,
    coverUrl = coverUrl,
    pageCount = pageCount,
    tags = listOf(Tag(id = -1, type = "local", name = "page:$lastReadPage", slug = "page"))
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    type = type,
    name = name,
    slug = slug,
    nameZh = nameZh
)

fun Tag.toEntity(nameZh: String? = null, count: Int = 0): TagEntity = TagEntity(
    id = id,
    type = type,
    name = name,
    slug = slug,
    nameZh = nameZh,
    count = count
)
