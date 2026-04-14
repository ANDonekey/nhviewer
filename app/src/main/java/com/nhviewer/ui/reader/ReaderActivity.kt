package com.nhviewer.ui.reader

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.imageLoader
import coil.request.ImageRequest
import com.nhviewer.R
import com.nhviewer.domain.model.AppSettings
import com.nhviewer.domain.model.GalleryDetail
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.ui.common.LoadState
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReaderActivity : AppCompatActivity() {
    private val viewModel: ReaderViewModel by viewModel()
    private val settingsRepository: SettingsRepository by inject()

    private lateinit var viewPager: ViewPager2
    private lateinit var continuousRecyclerView: RecyclerView
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
    private lateinit var orientationToggleButton: Button

    private lateinit var singlePageAdapter: ReaderPageAdapter
    private lateinit var continuousAdapter: ReaderPageAdapter
    private var pagerRecycler: RecyclerView? = null

    private var currentDetail: GalleryDetail? = null
    private var hasRestoredPosition = false
    private var requestedStartPage = 0
    private var isChromeVisible = true
    private var isSeeking = false
    private var currentPage = 1
    private var pageCount = 0
    private var isVerticalOrientation = false
    private var touchConfig = ReaderTouchConfig()

    private val chromeHandler = Handler(Looper.getMainLooper())
    private val autoHideChromeRunnable = Runnable {
        if (isChromeVisible && currentDetail != null) hideChrome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        viewPager = findViewById(R.id.viewPager)
        continuousRecyclerView = findViewById(R.id.continuousRecyclerView)
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
        orientationToggleButton = findViewById(R.id.orientationToggleButton)

        setupPagedReader()
        setupContinuousReader()
        setupControls()
        setupOrientationToggle()
        collectReaderTouchSettings()

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
        cancelAutoHideChrome()
    }

    override fun onDestroy() {
        cancelAutoHideChrome()
        super.onDestroy()
    }

    private fun setupPagedReader() {
        singlePageAdapter = ReaderPageAdapter(
            onSingleTap = { x, width -> handleTap(x, width) },
            onLongPress = {
                if (!touchConfig.gestureEnabled || isCurrentPageZoomedInSingle()) return@ReaderPageAdapter
                toggleReaderOrientation()
                scheduleAutoHideChrome()
            },
            onVerticalFling = { velocityY ->
                if (!touchConfig.gestureEnabled || isCurrentPageZoomedInSingle()) return@ReaderPageAdapter
                if (velocityY < 0f) showChrome() else hideChrome()
            }
        )
        viewPager.adapter = singlePageAdapter
        viewPager.offscreenPageLimit = 3
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (touchConfig.pagingMode == PagingMode.SINGLE) {
                    onPageChanged(position + 1)
                }
            }
        })
        pagerRecycler = viewPager.getChildAt(0) as? RecyclerView
    }

    private fun setupContinuousReader() {
        continuousAdapter = ReaderPageAdapter(
            layoutResId = R.layout.item_reader_page_continuous,
            onSingleTap = { x, width -> handleTap(x, width) },
            onLongPress = {
                if (!touchConfig.gestureEnabled || isCurrentPageZoomedInContinuous()) return@ReaderPageAdapter
                toggleReaderOrientation()
                scheduleAutoHideChrome()
            },
            onVerticalFling = { velocityY ->
                if (!touchConfig.gestureEnabled || isCurrentPageZoomedInContinuous()) return@ReaderPageAdapter
                if (velocityY < 0f) showChrome() else hideChrome()
            }
        )
        continuousRecyclerView.adapter = continuousAdapter
        continuousRecyclerView.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return touchConfig.pagingMode == PagingMode.CONTINUOUS &&
                    touchConfig.swipePagingEnabled &&
                    super.canScrollVertically()
            }
        }
        continuousRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (touchConfig.pagingMode != PagingMode.CONTINUOUS) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val first = lm.findFirstVisibleItemPosition()
                if (first != RecyclerView.NO_POSITION) {
                    onPageChanged(first + 1)
                }
            }
        })
    }

    private fun setupOrientationToggle() {
        orientationToggleButton.setOnClickListener {
            toggleReaderOrientation()
            scheduleAutoHideChrome()
        }
        orientationToggleButton.text = getString(R.string.reader_orientation_horizontal)
    }

    private fun toggleReaderOrientation() {
        if (touchConfig.pagingMode == PagingMode.CONTINUOUS) {
            Toast.makeText(this, R.string.reader_mode_continuous_tip, Toast.LENGTH_SHORT).show()
            return
        }
        isVerticalOrientation = !isVerticalOrientation
        viewPager.orientation = if (isVerticalOrientation) {
            ViewPager2.ORIENTATION_VERTICAL
        } else {
            ViewPager2.ORIENTATION_HORIZONTAL
        }
        orientationToggleButton.text = if (isVerticalOrientation) {
            getString(R.string.reader_orientation_vertical)
        } else {
            getString(R.string.reader_orientation_horizontal)
        }
    }

    private fun isCurrentPageZoomedInSingle(): Boolean {
        val recycler = pagerRecycler ?: return false
        val holder = recycler.findViewHolderForAdapterPosition(viewPager.currentItem)
            as? ReaderPageAdapter.ReaderPageViewHolder
        return holder?.isImageZoomed() == true
    }

    private fun isCurrentPageZoomedInContinuous(): Boolean {
        val holder = continuousRecyclerView.findViewHolderForAdapterPosition((currentPage - 1).coerceAtLeast(0))
            as? ReaderPageAdapter.ReaderPageViewHolder
        return holder?.isImageZoomed() == true
    }

    private fun handleTap(x: Float, width: Int) {
        if (width <= 0) return
        val leftBoundary = width * 0.32f
        val rightBoundary = width * 0.68f
        val reverseTap = if (touchConfig.leftHandedMode) true else touchConfig.reverseTapZones

        when {
            x < leftBoundary -> {
                if (!touchConfig.tapPagingEnabled) return
                if (reverseTap) goToPage(currentPage + 1, smoothScroll = true)
                else goToPage(currentPage - 1, smoothScroll = true)
            }

            x > rightBoundary -> {
                if (!touchConfig.tapPagingEnabled) return
                if (reverseTap) goToPage(currentPage - 1, smoothScroll = true)
                else goToPage(currentPage + 1, smoothScroll = true)
            }

            else -> {
                if (touchConfig.tapToToggleChromeEnabled) toggleChrome()
            }
        }
    }

    private fun collectReaderTouchSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.observeSettings().collect { settings ->
                    applyReaderTouchSettings(settings)
                }
            }
        }
    }

    private fun applyReaderTouchSettings(settings: AppSettings) {
        val previousMode = touchConfig.pagingMode
        touchConfig = ReaderTouchConfig(
            tapPagingEnabled = settings.readerTapPagingEnabled,
            swipePagingEnabled = settings.readerSwipePagingEnabled,
            tapToToggleChromeEnabled = settings.readerTapToToggleChromeEnabled,
            reverseTapZones = settings.readerReverseTapZones,
            gestureEnabled = settings.readerGestureEnabled,
            leftHandedMode = settings.readerLeftHandedMode,
            pagingMode = if (settings.readerPagingMode.equals("continuous", ignoreCase = true)) {
                PagingMode.CONTINUOUS
            } else {
                PagingMode.SINGLE
            }
        )
        viewPager.isUserInputEnabled = touchConfig.swipePagingEnabled && touchConfig.pagingMode == PagingMode.SINGLE
        updatePagingModeVisibility()
        if (previousMode != touchConfig.pagingMode && currentDetail != null) {
            goToPage(currentPage, smoothScroll = false)
        }
    }

    private fun updatePagingModeVisibility() {
        if (touchConfig.pagingMode == PagingMode.SINGLE) {
            viewPager.visibility = if (currentDetail == null) View.INVISIBLE else View.VISIBLE
            continuousRecyclerView.visibility = View.GONE
            orientationToggleButton.alpha = 1f
            orientationToggleButton.isEnabled = true
        } else {
            viewPager.visibility = View.GONE
            continuousRecyclerView.visibility = if (currentDetail == null) View.INVISIBLE else View.VISIBLE
            orientationToggleButton.alpha = 0.4f
            orientationToggleButton.isEnabled = false
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
                cancelAutoHideChrome()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val target = (seekBar?.progress ?: 0) + 1
                isSeeking = false
                goToPage(target, smoothScroll = false)
                scheduleAutoHideChrome()
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
                continuousRecyclerView.visibility = View.INVISIBLE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.reader_loading)
                messageView.setOnClickListener(null)
                setChromeEnabled(false)
            }

            is LoadState.Content -> {
                messageView.visibility = View.GONE
                currentDetail = detailState.value
                titleView.text = detailState.value.title
                singlePageAdapter.submitList(detailState.value.images)
                continuousAdapter.submitList(detailState.value.images)
                pageCount = detailState.value.images.size
                pageSeekBar.max = (pageCount - 1).coerceAtLeast(0)

                val preferredPage = maxOf(state.savedProgress, requestedStartPage)
                    .coerceAtLeast(1)
                    .coerceAtMost(pageCount.coerceAtLeast(1))

                updatePagingModeVisibility()
                if (!hasRestoredPosition) {
                    hasRestoredPosition = true
                    if (touchConfig.pagingMode == PagingMode.SINGLE) {
                        viewPager.post { goToPage(preferredPage, smoothScroll = false) }
                    } else {
                        continuousRecyclerView.post { goToPage(preferredPage, smoothScroll = false) }
                    }
                } else {
                    updatePageUi(currentPage)
                }

                setChromeEnabled(true)
                scheduleAutoHideChrome()
            }

            LoadState.Empty -> {
                viewPager.visibility = View.INVISIBLE
                continuousRecyclerView.visibility = View.INVISIBLE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.reader_empty)
                messageView.setOnClickListener(null)
                setChromeEnabled(false)
            }

            is LoadState.Error -> {
                viewPager.visibility = View.INVISIBLE
                continuousRecyclerView.visibility = View.INVISIBLE
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
        val normalizedPage = page.coerceIn(1, pageCount.coerceAtLeast(1))
        if (normalizedPage == currentPage) {
            updatePageUi(normalizedPage)
            return
        }
        currentPage = normalizedPage
        updatePageUi(normalizedPage)
        saveCurrentProgress()
        prefetchAround(normalizedPage)
        scheduleAutoHideChrome()
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
        if (touchConfig.pagingMode == PagingMode.SINGLE) {
            if (target == currentPage && viewPager.currentItem == target - 1) {
                updatePageUi(target)
                return
            }
            viewPager.setCurrentItem(target - 1, smoothScroll)
        } else {
            val lm = continuousRecyclerView.layoutManager as? LinearLayoutManager
            if (smoothScroll) {
                continuousRecyclerView.smoothScrollToPosition(target - 1)
            } else {
                lm?.scrollToPositionWithOffset(target - 1, 0)
            }
            onPageChanged(target)
        }
    }

    private fun saveCurrentProgress() {
        val detail = currentDetail ?: return
        if (currentPage <= 0) return
        viewModel.saveProgress(detail.id, currentPage, detail.title, detail.coverUrl, detail.pageCount)
    }

    private fun prefetchAround(page: Int) {
        val detail = currentDetail ?: return
        if (detail.images.isEmpty()) return
        val indices = (page - 2..page + 3)
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

    private fun showChrome() {
        if (isChromeVisible) {
            scheduleAutoHideChrome()
            return
        }
        isChromeVisible = true
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        scheduleAutoHideChrome()
    }

    private fun hideChrome() {
        if (!isChromeVisible) return
        isChromeVisible = false
        topBar.visibility = View.GONE
        bottomBar.visibility = View.GONE
        cancelAutoHideChrome()
    }

    private fun toggleChrome() {
        if (isChromeVisible) hideChrome() else showChrome()
    }

    private fun setChromeEnabled(enabled: Boolean) {
        if (!enabled) {
            cancelAutoHideChrome()
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

    private fun scheduleAutoHideChrome() {
        cancelAutoHideChrome()
        chromeHandler.postDelayed(autoHideChromeRunnable, 3000)
    }

    private fun cancelAutoHideChrome() {
        chromeHandler.removeCallbacks(autoHideChromeRunnable)
    }

    private data class ReaderTouchConfig(
        val tapPagingEnabled: Boolean = true,
        val swipePagingEnabled: Boolean = true,
        val tapToToggleChromeEnabled: Boolean = true,
        val reverseTapZones: Boolean = false,
        val gestureEnabled: Boolean = true,
        val leftHandedMode: Boolean = false,
        val pagingMode: PagingMode = PagingMode.SINGLE
    )

    private enum class PagingMode {
        SINGLE,
        CONTINUOUS
    }

    companion object {
        const val EXTRA_GALLERY_ID = "extra_gallery_id"
        const val EXTRA_START_PAGE = "extra_start_page"
    }
}
