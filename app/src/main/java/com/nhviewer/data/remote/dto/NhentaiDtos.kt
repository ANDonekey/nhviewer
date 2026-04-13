package com.nhviewer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GalleryListDto(
    @SerialName("result")
    val result: List<GalleryDto> = emptyList(),
    @SerialName("page")
    val page: Int = 1,
    @SerialName("num_pages")
    val numPages: Int? = null
)

@Serializable
data class GalleryDto(
    @SerialName("id")
    val id: Long,
    @SerialName("media_id")
    val mediaId: String? = null,
    @SerialName("english_title")
    val englishTitle: String? = null,
    @SerialName("japanese_title")
    val japaneseTitle: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("num_pages")
    val numPages: Int = 0,
    @SerialName("thumbnail")
    val thumbnail: String? = null,
    @SerialName("tag_ids")
    val tagIds: List<Long> = emptyList(),
    @SerialName("blacklisted")
    val blacklisted: Boolean = false
)

@Serializable
data class GalleryDetailDto(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: GalleryTitleDto = GalleryTitleDto(),
    @SerialName("num_pages")
    val numPages: Int = 0,
    @SerialName("cover")
    val cover: ImagePathDto? = null,
    @SerialName("thumbnail")
    val thumbnail: ImagePathDto? = null,
    @SerialName("pages")
    val pages: List<PageDto> = emptyList(),
    @SerialName("tags")
    val tags: List<TagDto> = emptyList()
)

@Serializable
data class GalleryTitleDto(
    @SerialName("english")
    val english: String? = null,
    @SerialName("japanese")
    val japanese: String? = null,
    @SerialName("pretty")
    val pretty: String? = null
)

@Serializable
data class ImagePathDto(
    @SerialName("path")
    val path: String? = null,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("height")
    val height: Int? = null
)

@Serializable
data class TagDto(
    @SerialName("id")
    val id: Long,
    @SerialName("type")
    val type: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("slug")
    val slug: String = ""
)

@Serializable
data class ImageDto(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("url")
    val url: String = ""
)

@Serializable
data class PageDto(
    @SerialName("number")
    val number: Int = 0,
    @SerialName("path")
    val path: String = "",
    @SerialName("thumbnail")
    val thumbnail: String? = null
)

@Serializable
data class TagSearchRequestDto(
    @SerialName("type")
    val type: String = "tag",
    @SerialName("query")
    val query: String? = null,
    @SerialName("limit")
    val limit: Int = 15
)

@Serializable
data class UserPublicDto(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("username")
    val username: String = "",
    @SerialName("slug")
    val slug: String = ""
)

@Serializable
data class GalleryCommentDto(
    @SerialName("id")
    val id: Long,
    @SerialName("gallery_id")
    val galleryId: Long,
    @SerialName("poster")
    val poster: UserPublicDto = UserPublicDto(),
    @SerialName("post_date")
    val postDate: Long = 0L,
    @SerialName("body")
    val body: String = ""
)

@Serializable
data class FavoriteActionDto(
    @SerialName("favorited")
    val favorited: Boolean = false
)

@Serializable
data class UserMeDto(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("username")
    val username: String = "",
    @SerialName("slug")
    val slug: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String = "",
    @SerialName("about")
    val about: String = "",
    @SerialName("favorite_tags")
    val favoriteTags: String = ""
)

@Serializable
data class UserRecentCommentDto(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("gallery_id")
    val galleryId: Long = 0,
    @SerialName("gallery_title")
    val galleryTitle: String = "",
    @SerialName("body")
    val body: String = "",
    @SerialName("post_date")
    val postDate: Long = 0
)

@Serializable
data class UserProfileDto(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("username")
    val username: String = "",
    @SerialName("slug")
    val slug: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String = "",
    @SerialName("about")
    val about: String = "",
    @SerialName("favorite_tags")
    val favoriteTags: String = "",
    @SerialName("date_joined")
    val dateJoined: Long = 0L,
    @SerialName("recent_favorites")
    val recentFavorites: List<GalleryDto> = emptyList(),
    @SerialName("recent_comments")
    val recentComments: List<UserRecentCommentDto> = emptyList()
)
