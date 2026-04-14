package com.nhviewer.ui.detail

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nhviewer.R
import com.nhviewer.download.DownloadCenter
import com.nhviewer.domain.model.GalleryComment
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.model.Tag
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.reader.ReaderActivity
import com.nhviewer.ui.search.SearchActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailActivity : AppCompatActivity() {
    private val detailViewModel: DetailViewModel by viewModel()

    private lateinit var titleView: TextView
    private lateinit var categoryView: TextView
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
    private var showChineseTags: Boolean = true
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
        val builder = SpannableStringBuilder()
        target.forEachIndexed { index, comment ->
            val timeText = timeFormatter.format(Instant.ofEpochSecond(comment.postDateSeconds))
            val header = "${comment.username}   $timeText"
            val headerSpan = SpannableString(header)
            headerSpan.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openCommentUserHome(comment)
                    }
                },
                0,
                comment.username.length.coerceAtLeast(0),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(headerSpan)
            builder.append('\n')
            builder.append(comment.body)
            if (index != target.lastIndex) {
                builder.append("\n\n")
            }
        }
        commentsView.text = builder
        commentsView.movementMethod = LinkMovementMethod.getInstance()

        val hasMore = allComments.size > 2
        commentsToggleView.visibility = if (hasMore) View.VISIBLE else View.GONE
        if (hasMore) {
            commentsToggleView.text = if (areCommentsExpanded) {
                getString(R.string.detail_comments_collapse)
            } else {
                getString(R.string.detail_comments_expand, allComments.size)
            }
        }
    }

    private fun openCommentUserHome(comment: GalleryComment) {
        val slug = comment.userSlug?.trim().orEmpty()
        if (slug.isBlank()) {
            Toast.makeText(this, R.string.detail_comment_user_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://nhentai.net/users/$slug/"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Throwable) {
            Toast.makeText(this, R.string.detail_comment_user_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailViewModel.settings.collect { settings ->
                    val titleChanged = preferJapaneseTitle != settings.preferJapaneseTitle
                    val tagDisplayChanged = showChineseTags != settings.showChineseTags
                    preferJapaneseTitle = settings.preferJapaneseTitle
                    showChineseTags = settings.showChineseTags
                    if (titleChanged) {
                        lastDetail?.let { detail ->
                            titleView.text = selectTitle(detail).ifBlank { getString(R.string.gallery_untitled, detail.id) }
                        }
                    }
                    if (tagDisplayChanged) {
                        lastDetail?.let { detail ->
                            categoryView.text = inferCategory(detail.tags)
                            bindTagSections(detail.tags)
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
                    text = displayTagName(tag)
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
        val candidate = tags.firstOrNull { it.type.equals("category", ignoreCase = true) }
            ?.let { displayTagName(it) }
        if (!candidate.isNullOrBlank()) return candidate.uppercase(Locale.getDefault())
        return getString(R.string.detail_category_default)
    }

    private fun displayTagName(tag: Tag): String {
        if (showChineseTags) {
            val zh = tag.nameZh?.trim().orEmpty()
            if (zh.isNotEmpty()) return zh
        }
        return tag.name
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
