package com.nhviewer.domain.model

data class GalleryComment(
    val id: Long,
    val galleryId: Long,
    val username: String,
    val body: String,
    val postDateSeconds: Long,
    val userId: Long? = null,
    val userSlug: String? = null
)
