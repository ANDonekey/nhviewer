package com.nhviewer.domain.model

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int
)
