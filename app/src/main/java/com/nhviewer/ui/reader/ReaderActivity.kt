package com.nhviewer.ui.reader

import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.imageLoader
import coil.request.ImageRequest
import com.nhviewer.R
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.common.NhViewModelFactory
import kotlinx.coroutines.launch

class ReaderActivity : AppCompatActivity() {
    private val viewModel: ReaderViewModel by viewModels { NhViewModelFactory() }

    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var titleView: TextView
    private lateinit var progressView: TextView
    private lateinit var messageView: TextView
    private lateinit var pageSeekBar: SeekBar
    private lateinit var firstPageButton: Button
    private lateinit var prevPageButton: Button
    private lateinit var jumpPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var lastPageButton: Button
    private lateinit var adapter: ReaderPageAdapter

    private var currentDetail: GalleryDetail? = null
    private var hasRestoredPosition = false
    private var requestedStartPage = 0
    private var isChromeVisible = true
    private var isSeeking = false
    private var currentPage = 1
    private var pageCount = 0

    private var touchDownX = 0f
    private var touchDownY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        titleView = findViewById(R.id.titleView)
        progressView = findViewById(R.id.progressView)
        messageView = findViewById(R.id.messageView)
        pageSeekBar = findViewById(R.id.pageSeekBar)
        firstPageButton = findViewById(R.id.firstPageButton)
        prevPageButton = findViewById(R.id.prevPageButton)
        jumpPageButton = findViewById(R.id.jumpPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        lastPageButton = findViewById(R.id.lastPageButton)

        adapter = ReaderPageAdapter()
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                onPageChanged(position + 1)
            }
        })
        setupTapZones()
        setupControls()

        val galleryId = intent.getLongExtra(EXTRA_GALLERY_ID, -1L)
        requestedStartPage = intent.getIntExtra(EXTRA_START_PAGE, 0)
        if (galleryId <= 0L) {
            finish()
            return
        }

        collectState()
        viewModel.load(galleryId)
    }

    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
    }

    private fun setupTapZones() {
        val pagerRecycler = viewPager.getChildAt(0) as? RecyclerView ?: return
        pagerRecycler.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownX = event.x
                    touchDownY = event.y
                }

                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.x - touchDownX)
                    val dy = kotlin.math.abs(event.y - touchDownY)
                    if (dx < 18f && dy < 18f) {
                        handleTap(event.x, pagerRecycler.width)
                    }
                }
            }
            false
        }
    }

    private fun handleTap(x: Float, width: Int) {
        if (width <= 0) return
        val left = width * 0.32f
        val right = width * 0.68f
        when {
            x < left -> goToPage(currentPage - 1, smoothScroll = true)
            x > right -> goToPage(currentPage + 1, smoothScroll = true)
            else -> toggleChrome()
        }
    }

    private fun setupControls() {
        firstPageButton.setOnClickListener { goToPage(1, smoothScroll = false) }
        prevPageButton.setOnClickListener { goToPage(currentPage - 1, smoothScroll = true) }
        nextPageButton.setOnClickListener { goToPage(currentPage + 1, smoothScroll = true) }
        lastPageButton.setOnClickListener { goToPage(pageCount, smoothScroll = false) }
        jumpPageButton.setOnClickListener { showJumpDialog() }

        pageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    progressView.text = getString(R.string.reader_progress, progress + 1, pageCount.coerceAtLeast(1))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val target = (seekBar?.progress ?: 0) + 1
                isSeeking = false
                goToPage(target, smoothScroll = false)
            }
        })
    }

    private fun showJumpDialog() {
        if (pageCount <= 0) return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.reader_jump_hint, pageCount)
            setText(currentPage.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.reader_jump_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val page = input.text?.toString()?.toIntOrNull()
                if (page == null || page !in 1..pageCount) {
                    Toast.makeText(this, R.string.reader_jump_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                goToPage(page, smoothScroll = false)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: ReaderUiState) {
        when (val detailState = state.detailState) {
            LoadState.Loading -> {
                viewPager.visibility = View.INVISIBLE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.reader_loading)
                messageView.setOnClickListener(null)
                setChromeEnabled(false)
            }

            is LoadState.Content -> {
                messageView.visibility = View.GONE
                viewPager.visibility = View.VISIBLE
                currentDetail = detailState.value
                titleView.text = detailState.value.title
                adapter.submitList(detailState.value.images)
                pageCount = detailState.value.images.size
                pageSeekBar.max = (pageCount - 1).coerceAtLeast(0)

                val preferredPage = maxOf(state.savedProgress, requestedStartPage).coerceAtLeast(1).coerceAtMost(pageCount.coerceAtLeast(1))
                if (!hasRestoredPosition) {
                    hasRestoredPosition = true
                    viewPager.post {
                        goToPage(preferredPage, smoothScroll = false)
                    }
                } else {
                    updatePageUi(currentPage)
                }

                setChromeEnabled(true)
            }

            LoadState.Empty -> {
                viewPager.visibility = View.INVISIBLE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.reader_empty)
                messageView.setOnClickListener(null)
                setChromeEnabled(false)
            }

            is LoadState.Error -> {
                viewPager.visibility = View.INVISIBLE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.reader_error, detailState.message) + "\n" + getString(R.string.reader_retry)
                messageView.setOnClickListener {
                    state.galleryId?.let { viewModel.load(it) }
                }
                setChromeEnabled(false)
            }
        }
    }

    private fun onPageChanged(page: Int) {
        currentPage = page
        updatePageUi(page)
        saveCurrentProgress()
        prefetchAround(page)
    }

    private fun updatePageUi(page: Int) {
        val total = pageCount.coerceAtLeast(1)
        progressView.text = getString(R.string.reader_progress, page, total)
        if (!isSeeking) {
            pageSeekBar.progress = (page - 1).coerceAtLeast(0)
        }
        firstPageButton.isEnabled = page > 1
        prevPageButton.isEnabled = page > 1
        nextPageButton.isEnabled = page < total
        lastPageButton.isEnabled = page < total
    }

    private fun goToPage(page: Int, smoothScroll: Boolean) {
        if (pageCount <= 0) return
        val target = page.coerceIn(1, pageCount)
        if (target == currentPage && viewPager.currentItem == target - 1) {
            updatePageUi(target)
            return
        }
        viewPager.setCurrentItem(target - 1, smoothScroll)
    }

    private fun saveCurrentProgress() {
        val detail = currentDetail ?: return
        if (currentPage <= 0) return
        viewModel.saveProgress(detail.id, currentPage, detail.title, detail.coverUrl, detail.pageCount)
    }

    private fun prefetchAround(page: Int) {
        val detail = currentDetail ?: return
        if (detail.images.isEmpty()) return

        val indices = listOf(page, page + 1, page + 2)
            .map { it - 1 }
            .filter { it in detail.images.indices }

        indices.forEach { index ->
            val url = detail.images[index].url
            imageLoader.enqueue(
                ImageRequest.Builder(this)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build()
            )
        }
    }

    private fun toggleChrome() {
        isChromeVisible = !isChromeVisible
        topBar.visibility = if (isChromeVisible) View.VISIBLE else View.GONE
        bottomBar.visibility = if (isChromeVisible) View.VISIBLE else View.GONE
    }

    private fun setChromeEnabled(enabled: Boolean) {
        if (!enabled) {
            topBar.alpha = 0.5f
            bottomBar.alpha = 0.5f
            firstPageButton.isEnabled = false
            prevPageButton.isEnabled = false
            jumpPageButton.isEnabled = false
            nextPageButton.isEnabled = false
            lastPageButton.isEnabled = false
            pageSeekBar.isEnabled = false
            return
        }

        topBar.alpha = 1f
        bottomBar.alpha = 1f
        jumpPageButton.isEnabled = true
        pageSeekBar.isEnabled = true
        updatePageUi(currentPage.coerceAtLeast(1))
    }

    companion object {
        const val EXTRA_GALLERY_ID = "extra_gallery_id"
        const val EXTRA_START_PAGE = "extra_start_page"
    }
}
