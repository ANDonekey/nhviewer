package com.nhviewer.ui.common

object ErrorText {
    fun fromMessage(raw: String?, fallback: String): String {
        val message = raw?.trim().orEmpty()
        if (message.isBlank()) return fallback
        return when {
            message.startsWith("HTTP 401") -> "Unauthorized (401): check API Key in settings."
            message.startsWith("HTTP 403") -> "Forbidden (403): access denied."
            message.startsWith("HTTP 429") -> "Too many requests (429): please retry later."
            message.startsWith("HTTP 5") -> "Server error: please retry later."
            message.contains("timeout", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("network", ignoreCase = true) ->
                "Network error: please check your connection."
            else -> message
        }
    }
}
