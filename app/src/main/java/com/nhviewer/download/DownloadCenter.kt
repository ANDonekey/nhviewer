package com.nhviewer.download

import android.content.Context
import android.content.Intent
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

    /**
     * Enqueue a gallery for download. Returns the existing taskId if the gallery is already
     * QUEUED, RUNNING, or PAUSED (de-dup guard). Otherwise creates a new task and starts it.
     */
    fun enqueueGallery(galleryId: Long, title: String, images: List<PageImage>): String {
        check(initialized) { "DownloadCenter not initialized" }

        // De-dup: don't re-enqueue if already active
        val existing = _tasks.value.firstOrNull {
            it.galleryId == galleryId && it.status in setOf(
                DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PAUSED
            )
        }
        if (existing != null) return existing.taskId

        val safeTitle = title.ifBlank { "gallery_$galleryId" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
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
        startService()
        startTask(taskId, images, outputDir)
        return taskId
    }

    fun pause(taskId: String) {
        runningJobs.remove(taskId)?.cancel()
        updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED, errorMessage = null) }
    }

    fun resume(taskId: String) {
        val task = _tasks.value.firstOrNull { it.taskId == taskId } ?: return
        if (task.status != DownloadStatus.PAUSED) return
        val images = task.imageUrls.mapIndexed { index, url ->
            PageImage(index = index + 1, url = url, thumbnailUrl = null)
        }
        val outputDir = File(task.outputDir)
        outputDir.mkdirs()
        startService()
        startTask(taskId, images, outputDir)
    }

    fun retry(taskId: String) {
        val task = _tasks.value.firstOrNull { it.taskId == taskId } ?: return
        if (task.status == DownloadStatus.RUNNING) return
        val images = task.imageUrls.mapIndexed { index, url ->
            PageImage(index = index + 1, url = url, thumbnailUrl = null)
        }
        val outputDir = File(task.outputDir)
        outputDir.mkdirs()
        startService()
        startTask(taskId, images, outputDir)
    }

    fun cancel(taskId: String) {
        runningJobs.remove(taskId)?.cancel()
        updateTask(taskId) { it.copy(status = DownloadStatus.CANCELED, errorMessage = null) }
    }

    private fun startTask(taskId: String, images: List<PageImage>, outputDir: File) {
        // Count files already downloaded so we show accurate initial progress
        val alreadyDone = images.count { pageImage ->
            val ext = guessExtension(pageImage.url)
            File(outputDir, "p${pageImage.index}.$ext").exists()
        }
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.RUNNING,
                completed = alreadyDone,
                total = images.size,
                errorMessage = null
            )
        }

        val job = scope.launch {
            try {
                images.forEachIndexed { index, pageImage ->
                    val ext = guessExtension(pageImage.url)
                    val outFile = File(outputDir, "p${pageImage.index}.$ext")
                    if (!outFile.exists()) {
                        downloadFile(pageImage.url, outFile)
                    }
                    updateTask(taskId) { current ->
                        current.copy(status = DownloadStatus.RUNNING, completed = index + 1, total = images.size)
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
                    current.copy(status = DownloadStatus.FAILED, errorMessage = throwable.message ?: "Download failed")
                }
            } finally {
                runningJobs.remove(taskId)
            }
        }
        runningJobs[taskId] = job
    }

    private fun startService() {
        val intent = Intent(appContext, DownloadService::class.java)
        appContext.startService(intent)
    }

    private fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.taskId == taskId) transform(it) else it } }
    }

    @Throws(IOException::class)
    private fun downloadFile(url: String, output: File) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty response body")
            output.outputStream().use { body.byteStream().copyTo(it) }
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
