package com.nhviewer.ui.detail

import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.ui.common.LoadState

data class DetailUiState(
    val galleryId: Long? = null,
    val detailState: LoadState<GalleryDetail> = LoadState.Loading,
    val commentsState: LoadState<List<GalleryComment>> = LoadState.Loading,
    val isFavorite: Boolean = false,
    val savedProgress: Int = 0
)
