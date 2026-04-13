package com.nhviewer.ui.reader

import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.ui.common.LoadState

data class ReaderUiState(
    val galleryId: Long? = null,
    val detailState: LoadState<GalleryDetail> = LoadState.Loading,
    val savedProgress: Int = 0
)
