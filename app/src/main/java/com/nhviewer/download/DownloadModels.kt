package com.nhviewer.download

data class DownloadTask(
    val taskId: String,
    val galleryId: Long,
    val title: String,
    val total: Int,
    val completed: Int,
    val status: DownloadStatus,
    val outputDir: String,
    val errorMessage: String? = null,
    val imageUrls: List<String> = emptyList()
)

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED
}
