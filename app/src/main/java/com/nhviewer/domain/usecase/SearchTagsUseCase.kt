package com.nhviewer.domain.usecase

import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.repository.GalleryRepository

class SearchTagsUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(keyword: String): Result<List<Tag>> {
        return galleryRepository.searchTags(keyword)
    }
}
