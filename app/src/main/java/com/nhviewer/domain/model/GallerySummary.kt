package com.nhviewer.domain.model

data class GallerySummary(
    val id: Long,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val coverUrl: String?,
    val pageCount: Int,
    val tags: List<Tag>,
    val tagIds: List<Long> = emptyList(),
    val blacklisted: Boolean = false
)
