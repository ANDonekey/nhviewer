package com.nhviewer.data.mapper

import com.nhviewer.data.remote.dto.GalleryDto
import com.nhviewer.data.remote.dto.GalleryListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryMapperTest {

    @Test
    fun `toDomainSummary maps blacklisted and tag ids`() {
        val dto = GalleryDto(
            id = 123L,
            englishTitle = "English Title",
            japaneseTitle = "Japanese Title",
            title = "Fallback",
            numPages = 25,
            thumbnail = "thumb.jpg",
            tagIds = listOf(1L, 2L, 3L),
            blacklisted = true
        )

        val model = dto.toDomainSummary()

        assertEquals(123L, model.id)
        assertEquals("English Title", model.title)
        assertTrue(model.blacklisted)
        assertEquals(listOf(1L, 2L, 3L), model.tagIds)
        assertTrue(model.coverUrl?.contains("thumb.jpg") == true)
    }

    @Test
    fun `toSummaryPage maps page and total pages`() {
        val dto = GalleryListDto(
            result = listOf(
                GalleryDto(
                    id = 1L,
                    englishTitle = "A",
                    numPages = 10,
                    thumbnail = "a.jpg",
                    blacklisted = false
                )
            ),
            page = 3,
            numPages = 9
        )

        val page = dto.toSummaryPage()

        assertEquals(3, page.page)
        assertEquals(9, page.totalPages)
        assertEquals(1, page.items.size)
        assertFalse(page.items.first().blacklisted)
    }
}
