package com.nhviewer.domain.model

data class UserMe(
    val id: Long,
    val username: String,
    val slug: String,
    val avatarUrl: String
)

data class UserProfile(
    val id: Long,
    val username: String,
    val slug: String,
    val avatarUrl: String,
    val about: String,
    val favoriteTags: String,
    val dateJoined: Long,
    val recentFavorites: List<GallerySummary>,
    val recentComments: List<UserRecentComment>
)

data class UserRecentComment(
    val id: Long,
    val galleryId: Long,
    val galleryTitle: String,
    val body: String,
    val postDate: Long
)
