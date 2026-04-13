package com.nhviewer.data.mapper

import com.nhviewer.data.remote.dto.GalleryDetailDto
import com.nhviewer.data.remote.dto.GalleryDto
import com.nhviewer.data.remote.dto.GalleryListDto
import com.nhviewer.data.remote.dto.GalleryCommentDto
import com.nhviewer.data.remote.dto.ImageDto
import com.nhviewer.data.remote.dto.PageDto
import com.nhviewer.data.remote.dto.TagDto
import com.nhviewer.data.remote.dto.UserMeDto
import com.nhviewer.data.remote.dto.UserProfileDto
import com.nhviewer.data.remote.dto.UserRecentCommentDto
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.PageImage
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.model.UserMe
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.domain.model.UserRecentComment

private const val THUMB_BASE_URL = "https://t1.nhentai.net/"
private const val IMAGE_BASE_URL = "https://i1.nhentai.net/"

fun GalleryListDto.toSummaryPage(): Page<GallerySummary> {
    return Page(
        items = result.map { it.toDomainSummary() },
        page = page,
        totalPages = numPages ?: page
    )
}

fun GalleryDto.toDomainSummary(): GallerySummary {
    val resolvedTitle = englishTitle
        ?.takeIf { it.isNotBlank() }
        ?: japaneseTitle?.takeIf { it.isNotBlank() }
        ?: title?.takeIf { it.isNotBlank() }
        ?: ""

    return GallerySummary(
        id = id,
        title = resolvedTitle,
        englishTitle = englishTitle,
        japaneseTitle = japaneseTitle,
        coverUrl = thumbnail?.let { path ->
            if (path.startsWith("http")) path else THUMB_BASE_URL + path
        },
        pageCount = numPages,
        tags = emptyList(),
        blacklisted = blacklisted
    )
}

fun GalleryDetailDto.toDomainDetail(): GalleryDetail {
    val resolvedTitle = title.english
        ?.takeIf { it.isNotBlank() }
        ?: title.pretty?.takeIf { it.isNotBlank() }
        ?: title.japanese?.takeIf { it.isNotBlank() }
        ?: ""

    val resolvedCoverUrl = thumbnail?.path?.let { path ->
        if (path.startsWith("http")) path else THUMB_BASE_URL + path
    } ?: cover?.path?.let { path ->
        if (path.startsWith("http")) path else IMAGE_BASE_URL + path
    }

    return GalleryDetail(
        id = id,
        title = resolvedTitle,
        englishTitle = title.english,
        japaneseTitle = title.japanese,
        coverUrl = resolvedCoverUrl,
        pageCount = numPages,
        images = pages.map { it.toDomain() },
        tags = tags.map { it.toDomain() }
    )
}

fun TagDto.toDomain(): Tag = Tag(
    id = id,
    type = type,
    name = name,
    slug = slug
)

fun GalleryCommentDto.toDomain(): GalleryComment = GalleryComment(
    id = id,
    galleryId = galleryId,
    username = poster.username.ifBlank { "unknown" },
    body = body,
    postDateSeconds = postDate
)

fun ImageDto.toDomain(): PageImage = PageImage(
    index = index,
    url = url,
    thumbnailUrl = null
)

fun PageDto.toDomain(): PageImage = PageImage(
    index = number,
    url = if (path.startsWith("http")) path else IMAGE_BASE_URL + path,
    thumbnailUrl = thumbnail?.let { thumbPath ->
        if (thumbPath.startsWith("http")) thumbPath else THUMB_BASE_URL + thumbPath
    }
)

fun UserMeDto.toDomain(): UserMe = UserMe(
    id = id,
    username = username,
    slug = slug,
    avatarUrl = avatarUrl
)

fun UserRecentCommentDto.toDomain(): UserRecentComment = UserRecentComment(
    id = id,
    galleryId = galleryId,
    galleryTitle = galleryTitle,
    body = body,
    postDate = postDate
)

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    slug = slug,
    avatarUrl = avatarUrl,
    about = about,
    favoriteTags = favoriteTags,
    dateJoined = dateJoined,
    recentFavorites = recentFavorites.map { it.toDomainSummary() },
    recentComments = recentComments.map { it.toDomain() }
)
