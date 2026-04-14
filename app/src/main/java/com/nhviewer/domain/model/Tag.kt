package com.nhviewer.domain.model

data class Tag(
    val id: Long,
    val type: String,
    val name: String,
    val slug: String,
    val nameZh: String? = null
)
