package com.nhviewer.domain.model

data class GalleryDetail(
    val id: Long,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val coverUrl: String?,
    val pageCount: Int,
    val images: List<PageImage>,
    val tags: List<Tag>
)

data class PageImage(
    val index: Int,
    val url: String,
    val thumbnailUrl: String?
)
