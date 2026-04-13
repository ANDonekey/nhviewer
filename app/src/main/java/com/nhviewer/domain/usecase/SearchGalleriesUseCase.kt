package com.nhviewer.domain.usecase

import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Page
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.domain.repository.GalleryRepository

class SearchGalleriesUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(
        query: String,
        page: Int,
        sort: SortOption,
        tags: List<Tag>
    ): Result<Page<GallerySummary>> {
        return galleryRepository.search(
            query = query,
            page = page,
            sort = sort,
            tags = tags
        )
    }
}
