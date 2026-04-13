package com.nhviewer.ui.detail

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nhviewer.app.AppGraph
import com.nhviewer.R
import com.nhviewer.download.DownloadCenter
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.Tag
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.common.NhViewModelFactory
import com.nhviewer.ui.reader.ReaderActivity
import com.nhviewer.ui.search.SearchActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {
    private val detailViewModel: DetailViewModel by viewModels { NhViewModelFactory() }

    private lateinit var titleView: TextView
    private lateinit var categoryView: TextView
    private lateinit var languageView: TextView
    private lateinit var pagesView: TextView
    private lateinit var favoriteCountView: TextView
    private lateinit var uploadTimeView: TextView
    private lateinit var commentsView: TextView
    private lateinit var commentsToggleView: TextView
    private lateinit var tagSectionContainer: LinearLayout
    private lateinit var coverView: ImageView
    private lateinit var previewRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageView: TextView
    private lateinit var readButton: Button
    private lateinit var favoriteButton: Button
    private lateinit var downloadButton: Button
    private lateinit var shareButton: Button
    private lateinit var previewAdapter: PreviewImageAdapter

    private var preferJapaneseTitle: Boolean = false
    private var lastDetail: GalleryDetail? = null
    private var currentComments: List<GalleryComment> = emptyList()
    private var areCommentsExpanded: Boolean = false

    private val timeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        titleView = findViewById(R.id.titleView)
        categoryView = findViewById(R.id.categoryView)
        languageView = findViewById(R.id.languageView)
        pagesView = findViewById(R.id.pagesView)
        favoriteCountView = findViewById(R.id.favoriteCountView)
        uploadTimeView = findViewById(R.id.uploadTimeView)
        commentsView = findViewById(R.id.commentsView)
        commentsToggleView = findViewById(R.id.commentsToggleView)
        tagSectionContainer = findViewById(R.id.tagSectionContainer)
        coverView = findViewById(R.id.coverView)
        previewRecyclerView = findViewById(R.id.previewRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        messageView = findViewById(R.id.messageView)
        readButton = findViewById(R.id.readButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        downloadButton = findViewById(R.id.downloadButton)
        shareButton = findViewById(R.id.shareButton)

        val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, -1L)
        if (galleryId <= 0L) {
            finish()
            return
        }

        previewAdapter = PreviewImageAdapter()
        previewAdapter.setOnItemClickListener { _, page ->
            startReader(galleryId, page)
        }
        previewRecyclerView.layoutManager = GridLayoutManager(this, 3)
        previewRecyclerView.adapter = previewAdapter
        previewRecyclerView.isNestedScrollingEnabled = false

        collectState()
        collectSettings()
        detailViewModel.loadDetail(galleryId)

        favoriteButton.setOnClickListener {
            detailViewModel.toggleFavorite()
        }
        readButton.setOnClickListener {
            val startPage = detailViewModel.uiState.value.savedProgress
            startReader(galleryId, startPage)
        }
        downloadButton.setOnClickListener {
            val detail = (detailViewModel.uiState.value.detailState as? LoadState.Content)?.value ?: return@setOnClickListener
            DownloadCenter.enqueueGallery(
                galleryId = detail.id,
                title = detail.title,
                images = detail.images
            )
        }
        shareButton.setOnClickListener {
            val shareId = detailViewModel.uiState.value.galleryId ?: galleryId
            val shareText = "https://nhentai.net/g/$shareId/"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }, getString(R.string.detail_share)))
        }
        commentsToggleView.setOnClickListener {
            areCommentsExpanded = !areCommentsExpanded
            renderCommentList(currentComments)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailViewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: DetailUiState) {
        when (val detailState = state.detailState) {
            LoadState.Loading -> {
                progressBar.visibility = View.VISIBLE
                messageView.visibility = View.GONE
                previewRecyclerView.visibility = View.GONE
                setActionEnabled(false)
            }

            is LoadState.Content -> {
                progressBar.visibility = View.GONE
                messageView.visibility = View.GONE
                previewRecyclerView.visibility = View.VISIBLE
                lastDetail = detailState.value
                bindDetail(detailState.value)
                setActionEnabled(true)
            }

            LoadState.Empty -> {
                progressBar.visibility = View.GONE
                previewRecyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.detail_empty)
                setActionEnabled(false)
            }

            is LoadState.Error -> {
                progressBar.visibility = View.GONE
                previewRecyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.detail_error, detailState.message)
                messageView.setOnClickListener {
                    detailViewModel.uiState.value.galleryId?.let { detailViewModel.loadDetail(it) }
                }
                setActionEnabled(false)
            }
        }

        favoriteButton.text = if (state.isFavorite) {
            getString(R.string.detail_favorite_remove)
        } else {
            getString(R.string.detail_favorite_add)
        }

        readButton.text = if (state.savedProgress > 1) {
            getString(R.string.detail_continue_read, state.savedProgress)
        } else {
            getString(R.string.detail_read)
        }

        renderComments(state.commentsState)
    }

    private fun bindDetail(detail: GalleryDetail) {
        titleView.text = selectTitle(detail).ifBlank { getString(R.string.gallery_untitled, detail.id) }
        categoryView.text = inferCategory(detail.tags)
        languageView.text = getString(R.string.detail_language_template, inferLanguage(detail.tags))
        pagesView.text = getString(R.string.detail_pages_template, detail.pageCount)
        favoriteCountView.text = getString(R.string.detail_favorites_template, detail.tags.size)
        uploadTimeView.text = getString(R.string.detail_upload_unknown)

        bindTagSections(detail.tags)
        coverView.load(detail.coverUrl) {
            crossfade(true)
            placeholder(ColorDrawable(0xFFD0D0D0.toInt()))
            error(ColorDrawable(0xFFD0D0D0.toInt()))
        }
        previewAdapter.submitList(detail.images)
    }

    private fun renderComments(state: LoadState<List<GalleryComment>>) {
        when (state) {
            LoadState.Loading -> {
                areCommentsExpanded = false
                currentComments = emptyList()
                commentsView.text = getString(R.string.detail_comments_loading)
                commentsToggleView.visibility = View.GONE
            }

            is LoadState.Content -> {
                currentComments = state.value
                renderCommentList(state.value)
            }

            LoadState.Empty -> {
                commentsView.text = getString(R.string.detail_no_comments)
                commentsToggleView.visibility = View.GONE
            }

            is LoadState.Error -> {
                commentsView.text = getString(R.string.detail_comments_error, state.message)
                commentsToggleView.visibility = View.GONE
            }
        }
    }

    private fun renderCommentList(allComments: List<GalleryComment>) {
        val target = if (areCommentsExpanded) allComments else allComments.take(2)
        commentsView.text = target.joinToString("\n\n") { comment ->
            val timeText = timeFormatter.format(Instant.ofEpochSecond(comment.postDateSeconds))
            "${comment.username}   $timeText\n${comment.body}"
        }
        val hasMore = allComments.size > 2
        commentsToggleView.visibility = if (hasMore) View.VISIBLE else View.GONE
        if (hasMore) {
            commentsToggleView.text = if (areCommentsExpanded) {
                getString(R.string.detail_comments_collapse)
            } else {
                getString(R.string.detail_comments_expand, allComments.size - 2)
            }
        }
    }

    private fun collectSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settingsRepository.observeSettings().collect { settings ->
                    val changed = preferJapaneseTitle != settings.preferJapaneseTitle
                    preferJapaneseTitle = settings.preferJapaneseTitle
                    if (changed) {
                        lastDetail?.let { detail ->
                            titleView.text = selectTitle(detail).ifBlank { getString(R.string.gallery_untitled, detail.id) }
                        }
                    }
                }
            }
        }
    }

    private fun selectTitle(detail: GalleryDetail): String {
        return if (preferJapaneseTitle) {
            detail.japaneseTitle?.takeIf { it.isNotBlank() }
                ?: detail.englishTitle?.takeIf { it.isNotBlank() }
                ?: detail.title
        } else {
            detail.englishTitle?.takeIf { it.isNotBlank() }
                ?: detail.japaneseTitle?.takeIf { it.isNotBlank() }
                ?: detail.title
        }
    }

    private fun setActionEnabled(enabled: Boolean) {
        readButton.isEnabled = enabled
        favoriteButton.isEnabled = enabled
        downloadButton.isEnabled = enabled
        shareButton.isEnabled = enabled
    }

    private fun bindTagSections(tags: List<Tag>) {
        tagSectionContainer.removeAllViews()
        if (tags.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = getString(R.string.detail_tags_empty)
                textSize = 14f
            }
            tagSectionContainer.addView(emptyView)
            return
        }

        val grouped = tags.groupBy { it.type.ifBlank { "other" } }.toSortedMap()
        grouped.entries.forEach { (type, items) ->
            val header = TextView(this).apply {
                text = type.replaceFirstChar { c -> c.uppercaseChar() }
                textSize = 13f
                setPadding(0, dp(2), 0, dp(4))
            }
            tagSectionContainer.addView(header)

            val chipGroup = ChipGroup(this).apply {
                isSingleLine = false
                chipSpacingHorizontal = dp(6)
                chipSpacingVertical = dp(6)
                setPadding(0, 0, 0, dp(8))
            }

            items.forEach { tag ->
                val chip = Chip(this).apply {
                    text = tag.name
                    isCheckable = false
                    isClickable = true
                    setEnsureMinTouchTargetSize(false)
                    chipMinHeight = 30f
                    setOnClickListener {
                        startActivity(Intent(this@DetailActivity, SearchActivity::class.java).apply {
                            putExtra(SearchActivity.EXTRA_PRESELECT_TAG_ID, tag.id)
                            putExtra(SearchActivity.EXTRA_PRESELECT_TAG_TYPE, tag.type)
                            putExtra(SearchActivity.EXTRA_PRESELECT_TAG_NAME, tag.name)
                            putExtra(SearchActivity.EXTRA_PRESELECT_TAG_SLUG, tag.slug)
                        })
                    }
                }
                chipGroup.addView(chip)
            }

            tagSectionContainer.addView(chipGroup)
        }
    }

    private fun inferCategory(tags: List<Tag>): String {
        val candidate = tags.firstOrNull { it.type.equals("category", ignoreCase = true) }?.name
        if (!candidate.isNullOrBlank()) return candidate.uppercase(Locale.getDefault())
        return getString(R.string.detail_category_default)
    }

    private fun inferLanguage(tags: List<Tag>): String {
        val names = tags.map { it.name.lowercase(Locale.getDefault()) }
        return when {
            names.any { it.contains("chinese") } -> "Chinese"
            names.any { it.contains("japanese") } -> "Japanese"
            names.any { it.contains("english") } -> "English"
            else -> "--"
        }
    }

    private fun startReader(galleryId: Long, page: Int) {
        startActivity(Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_GALLERY_ID, galleryId)
            putExtra(ReaderActivity.EXTRA_START_PAGE, page)
        })
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_GALLERY_ID = "extra_gallery_id"
    }
}
