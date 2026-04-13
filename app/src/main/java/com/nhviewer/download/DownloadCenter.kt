package com.nhviewer.download

import android.content.Context
import com.nhviewer.domain.model.PageImage
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

object DownloadCenter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val okHttpClient = OkHttpClient()
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val runningJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private lateinit var appContext: Context
    private var initialized = false

    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
    }

    fun enqueueGallery(
        galleryId: Long,
        title: String,
        images: List<PageImage>
    ): String {
        check(initialized) { "DownloadCenter not initialized" }
        val safeTitle = title.ifBlank { "gallery_$galleryId" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val outputDir = File(appContext.getExternalFilesDir(null), "nhviewer/$galleryId-$safeTitle")
        outputDir.mkdirs()

        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            taskId = taskId,
            galleryId = galleryId,
            title = title,
            total = images.size,
            completed = 0,
            status = DownloadStatus.QUEUED,
            outputDir = outputDir.absolutePath,
            imageUrls = images.map { it.url }
        )
        _tasks.update { listOf(task) + it }
        startTask(taskId, galleryId, title, images, outputDir)
        return taskId
    }

    fun retry(taskId: String, images: List<PageImage>) {
        val task = _tasks.value.firstOrNull { it.taskId == taskId } ?: return
        if (task.status == DownloadStatus.RUNNING) return
        val outputDir = File(task.outputDir)
        outputDir.mkdirs()
        startTask(taskId, task.galleryId, task.title, images, outputDir)
    }

    fun retry(taskId: String) {
        val task = _tasks.value.firstOrNull { it.taskId == taskId } ?: return
        val images = task.imageUrls.mapIndexed { index, url ->
            PageImage(index = index + 1, url = url, thumbnailUrl = null)
        }
        retry(taskId, images)
    }

    fun cancel(taskId: String) {
        runningJobs.remove(taskId)?.cancel()
        updateTask(taskId) { it.copy(status = DownloadStatus.CANCELED, errorMessage = null) }
    }

    private fun startTask(
        taskId: String,
        galleryId: Long,
        title: String,
        images: List<PageImage>,
        outputDir: File
    ) {
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.RUNNING,
                completed = 0,
                total = images.size,
                errorMessage = null
            )
        }

        val job = scope.launch {
            try {
                var completed = 0
                images.forEachIndexed { index, pageImage ->
                    val ext = guessExtension(pageImage.url)
                    val outFile = File(outputDir, "p${index + 1}.$ext")
                    downloadFile(pageImage.url, outFile)
                    completed += 1
                    updateTask(taskId) { current ->
                        current.copy(
                            status = DownloadStatus.RUNNING,
                            completed = completed,
                            total = images.size
                        )
                    }
                }
                updateTask(taskId) { current ->
                    current.copy(
                        status = DownloadStatus.COMPLETED,
                        completed = images.size,
                        total = images.size,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is kotlinx.coroutines.CancellationException) return@launch
                updateTask(taskId) { current ->
                    current.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = throwable.message ?: "Download failed"
                    )
                }
            } finally {
                runningJobs.remove(taskId)
            }
        }

        runningJobs[taskId] = job
    }

    private fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { task ->
                if (task.taskId == taskId) transform(task) else task
            }
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(url: String, output: File) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            val body = response.body ?: throw IOException("Empty response body")
            output.outputStream().use { stream ->
                body.byteStream().copyTo(stream)
            }
        }
    }

    private fun guessExtension(url: String): String {
        val clean = url.substringBefore('?')
        val ext = clean.substringAfterLast('.', "jpg").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif" -> ext
            else -> "jpg"
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
