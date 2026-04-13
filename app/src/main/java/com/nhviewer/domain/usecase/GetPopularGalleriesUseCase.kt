package com.nhviewer.domain.usecase

import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.repository.GalleryRepository

class GetPopularGalleriesUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(page: Int): Result<Page<GallerySummary>> {
        return galleryRepository.getPopular(page)
    }
}
