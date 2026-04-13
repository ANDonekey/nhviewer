package com.nhviewer.domain.usecase

import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.repository.GalleryRepository

class GetGalleryCommentsUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(galleryId: Long): Result<List<GalleryComment>> {
        return galleryRepository.getComments(galleryId)
    }
}
