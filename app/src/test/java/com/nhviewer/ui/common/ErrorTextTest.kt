package com.nhviewer.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTextTest {

    @Test
    fun `maps 401 to api key hint`() {
        val text = ErrorText.fromMessage("HTTP 401: Unauthorized", "fallback")
        assertEquals("Unauthorized (401): check API Key in settings.", text)
    }

    @Test
    fun `maps 429 to rate-limit hint`() {
        val text = ErrorText.fromMessage("HTTP 429: Too Many Requests", "fallback")
        assertEquals("Too many requests (429): please retry later.", text)
    }

    @Test
    fun `maps network wording to network hint`() {
        val text = ErrorText.fromMessage("Unable to resolve host api", "fallback")
        assertEquals("Network error: please check your connection.", text)
    }
}
