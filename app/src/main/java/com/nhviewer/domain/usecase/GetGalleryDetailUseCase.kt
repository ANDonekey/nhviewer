package com.nhviewer.domain.usecase

import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.repository.GalleryRepository

class GetGalleryDetailUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(galleryId: Long): Result<GalleryDetail> {
        return galleryRepository.getDetail(galleryId)
    }
}
