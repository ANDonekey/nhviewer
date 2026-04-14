package com.nhviewer.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nhviewer.R
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.SortOption
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.download.DownloadsActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import com.nhviewer.ui.home.HomeLanguageFilter
import com.nhviewer.ui.home.HomeViewModel
import com.nhviewer.ui.library.FavoritesActivity
import com.nhviewer.ui.library.HistoryActivity
import com.nhviewer.ui.search.SearchActivity
import com.nhviewer.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModel()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageView: TextView
    private lateinit var pageInfoView: TextView
    private lateinit var edgeHintBar: TextView
    private lateinit var initialLoadingView: ProgressBar
    private lateinit var pagingFooterBar: View
    private lateinit var pagingFooterProgress: ProgressBar
    private lateinit var pagingFooterText: TextView
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var menuButton: ImageButton
    private lateinit var addButton: ImageButton
    private lateinit var toolbarTitleView: TextView
    private lateinit var quickLanguageButton: MaterialButton
    private lateinit var quickSortButton: MaterialButton
    private lateinit var quickBlacklistButton: MaterialButton
    private lateinit var drawerHomeButton: View
    private lateinit var drawerPopularButton: View
    private lateinit var drawerFavoritesButton: View
    private lateinit var drawerHistoryButton: View
    private lateinit var drawerDownloadsButton: View
    private lateinit var drawerSettingsButton: View

    private lateinit var adapter: HomeGalleryAdapter
    private var filterAppliedFromSettings = false
    private var autoNextPagingTriggered = false
    private var autoPrevPagingTriggered = false
    private var edgeReadyForNextPage = false
    private var edgeReadyForPrevPage = false
    private var edgeGestureStartY = 0f
    private var edgeGestureDirection: EdgeGestureDirection? = null
    private var edgeGestureArmed = false
    private var pendingPageRequest: Int? = null
    private var pagingFooterState: PagingFooterState = PagingFooterState.HIDDEN
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideEdgeHintRunnable = Runnable { edgeHintBar.visibility = View.GONE }
    private val hidePagingFooterRunnable = Runnable { showPagingFooter(PagingFooterState.HIDDEN) }

    private enum class EdgeGestureDirection {
        NEXT_PAGE,
        PREV_PAGE
    }

    private enum class PagingFooterState {
        HIDDEN,
        LOADING,
        RETRY,
        END
    }

    companion object {
        private const val PAGING_LOG_TAG = "NHV-Paging"
        private const val MENU_ADD_FAV = 1
        private const val MENU_OPEN_DETAIL = 2
        private const val MENU_ENTRY_HOME = 101
        private const val MENU_ENTRY_POPULAR = 102
        private const val MENU_ENTRY_RANDOM = 103
        private const val MENU_ENTRY_FAVORITES = 104
        private const val MENU_ENTRY_HISTORY = 105
        private const val MENU_ENTRY_SETTINGS = 106
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawerLayout)
        recyclerView = findViewById(R.id.recyclerView)
        messageView = findViewById(R.id.messageView)
        pageInfoView = findViewById(R.id.pageInfoView)
        edgeHintBar = findViewById(R.id.edgeHintBar)
        initialLoadingView = findViewById(R.id.initialLoadingView)
        pagingFooterBar = findViewById(R.id.pagingFooterBar)
        pagingFooterProgress = findViewById(R.id.pagingFooterProgress)
        pagingFooterText = findViewById(R.id.pagingFooterText)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        menuButton = findViewById(R.id.menuButton)
        addButton = findViewById(R.id.addButton)
        toolbarTitleView = findViewById(R.id.toolbarTitleView)
        quickLanguageButton = findViewById(R.id.quickLanguageButton)
        quickSortButton = findViewById(R.id.quickSortButton)
        quickBlacklistButton = findViewById(R.id.quickBlacklistButton)
        drawerHomeButton = findViewById(R.id.drawerHomeButton)
        drawerPopularButton = findViewById(R.id.drawerPopularButton)
        drawerFavoritesButton = findViewById(R.id.drawerFavoritesButton)
        drawerHistoryButton = findViewById(R.id.drawerHistoryButton)
        drawerDownloadsButton = findViewById(R.id.drawerDownloadsButton)
        drawerSettingsButton = findViewById(R.id.drawerSettingsButton)
        toolbarTitleView.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }

        adapter = HomeGalleryAdapter(
            onItemClick = { item ->
                startActivity(
                    Intent(this, DetailActivity::class.java).apply {
                        putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
                    }
                )
            },
            onItemLongClick = { item, anchor -> showItemMenu(item, anchor) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupAutoPaging()

        menuButton.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        addButton.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        quickLanguageButton.setOnClickListener { cycleLanguageFilter() }
        quickSortButton.setOnClickListener { cycleSortOption() }
        quickBlacklistButton.setOnClickListener { toggleBlacklistFilter() }

        drawerHomeButton.setOnClickListener {
            homeViewModel.persistHomeSortOption(SortOption.RECENT)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerPopularButton.setOnClickListener {
            homeViewModel.persistHomeSortOption(SortOption.POPULAR)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerFavoritesButton.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerDownloadsButton.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        drawerSettingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        pagingFooterBar.setOnClickListener {
            if (pagingFooterState == PagingFooterState.RETRY) {
                val targetPage = pendingPageRequest ?: homeViewModel.uiState.value.currentPage
                requestPage(targetPage)
            }
        }

        prevPageButton.setOnClickListener {
            val current = homeViewModel.uiState.value.currentPage
            if (current > 1) {
                requestPage(current - 1)
            } else {
                showPagingFooter(PagingFooterState.END, autoHideMs = 1400L)
            }
        }
        nextPageButton.setOnClickListener {
            val current = homeViewModel.uiState.value.currentPage
            val total = homeViewModel.uiState.value.totalPages
            if (current < total) {
                requestPage(current + 1)
            } else {
                showPagingFooter(PagingFooterState.END, autoHideMs = 1400L)
            }
        }
        pageInfoView.setOnClickListener { showJumpToPageDialog() }

        collectState()
        collectSettingsAndFilters()
    }

    override fun onStop() {
        super.onStop()
        uiHandler.removeCallbacks(hideEdgeHintRunnable)
        uiHandler.removeCallbacks(hidePagingFooterRunnable)
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        homeViewModel.savedScrollIndex = lm.findFirstVisibleItemPosition()
        homeViewModel.savedScrollOffset =
            recyclerView.findViewHolderForAdapterPosition(homeViewModel.savedScrollIndex)
                ?.itemView?.top ?: 0
    }

    private fun showItemMenu(item: GallerySummary, anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_ADD_FAV, 0, R.string.menu_add_favorite)
            menu.add(0, MENU_OPEN_DETAIL, 1, R.string.menu_open_detail)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    MENU_ADD_FAV -> {
                        homeViewModel.addToLocalFavorites(item)
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_added_favorite,
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    MENU_OPEN_DETAIL -> {
                        startActivity(
                            Intent(this@MainActivity, DetailActivity::class.java).apply {
                                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
                            }
                        )
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showQuickEntryMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_ENTRY_HOME, 0, getString(R.string.home_sort_recent))
            menu.add(0, MENU_ENTRY_POPULAR, 1, getString(R.string.home_sort_popular))
            menu.add(0, MENU_ENTRY_RANDOM, 2, getString(R.string.home_sort_random))
            menu.add(0, MENU_ENTRY_FAVORITES, 3, getString(R.string.nav_favorites))
            menu.add(0, MENU_ENTRY_HISTORY, 4, getString(R.string.nav_history))
            menu.add(0, MENU_ENTRY_SETTINGS, 5, getString(R.string.nav_settings))
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    MENU_ENTRY_HOME -> {
                        val current = homeViewModel.uiState.value
                        homeViewModel.applySettingsFilters(
                            languageFilter = current.languageFilter,
                            sortOption = SortOption.RECENT,
                            hideBlacklisted = current.hideBlacklisted
                        )
                        homeViewModel.persistHomeSortOption(SortOption.RECENT)
                        true
                    }
                    MENU_ENTRY_POPULAR -> {
                        val current = homeViewModel.uiState.value
                        homeViewModel.applySettingsFilters(
                            languageFilter = current.languageFilter,
                            sortOption = SortOption.POPULAR,
                            hideBlacklisted = current.hideBlacklisted
                        )
                        homeViewModel.persistHomeSortOption(SortOption.POPULAR)
                        true
                    }
                    MENU_ENTRY_RANDOM -> {
                        val current = homeViewModel.uiState.value
                        homeViewModel.applySettingsFilters(
                            languageFilter = current.languageFilter,
                            sortOption = SortOption.RANDOM,
                            hideBlacklisted = current.hideBlacklisted
                        )
                        homeViewModel.persistHomeSortOption(SortOption.RANDOM)
                        true
                    }
                    MENU_ENTRY_FAVORITES -> {
                        startActivity(Intent(this@MainActivity, FavoritesActivity::class.java))
                        true
                    }
                    MENU_ENTRY_HISTORY -> {
                        startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                        true
                    }
                    MENU_ENTRY_SETTINGS -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun setupAutoPaging() {
        val edgeTriggerThresholdPx = resources.displayMetrics.density * 72f

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val state = homeViewModel.uiState.value
                if (state.galleryListState !is LoadState.Content) return
                if (dy != 0) {
                    Log.d(
                        PAGING_LOG_TAG,
                        "onScrolled dy=$dy canDown=${recyclerView.canScrollVertically(1)} canUp=${recyclerView.canScrollVertically(-1)} page=${state.currentPage}/${state.totalPages}"
                    )
                }
                if (dy > 0 && !recyclerView.canScrollVertically(1) && state.currentPage < state.totalPages) {
                    if (!edgeReadyForNextPage) {
                        edgeReadyForNextPage = true
                        Log.d(PAGING_LOG_TAG, "edgeReadyForNextPage=true")
                        showPagingHint(getString(R.string.home_hint_edge_next))
                    }
                } else if (dy > 0 && !recyclerView.canScrollVertically(1) && state.currentPage >= state.totalPages) {
                    Log.d(PAGING_LOG_TAG, "hit bottom but already last page=${state.currentPage}")
                    showPagingFooter(PagingFooterState.END, autoHideMs = 1400L)
                } else if (recyclerView.canScrollVertically(1)) {
                    edgeReadyForNextPage = false
                }

                if (dy < 0 && !recyclerView.canScrollVertically(-1) && state.currentPage > 1) {
                    if (!edgeReadyForPrevPage) {
                        edgeReadyForPrevPage = true
                        Log.d(PAGING_LOG_TAG, "edgeReadyForPrevPage=true")
                        showPagingHint(getString(R.string.home_hint_edge_prev))
                    }
                } else if (dy < 0 && !recyclerView.canScrollVertically(-1) && state.currentPage <= 1) {
                    Log.d(PAGING_LOG_TAG, "hit top but already first page=${state.currentPage}")
                    showPagingFooter(PagingFooterState.END, autoHideMs = 1400L)
                } else if (recyclerView.canScrollVertically(-1)) {
                    edgeReadyForPrevPage = false
                }
            }
        })

        recyclerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    edgeGestureStartY = event.y
                    edgeGestureArmed = false
                    val state = homeViewModel.uiState.value
                    edgeGestureDirection = when {
                        !recyclerView.canScrollVertically(1) && state.currentPage < state.totalPages -> {
                            EdgeGestureDirection.NEXT_PAGE
                        }
                        !recyclerView.canScrollVertically(-1) && state.currentPage > 1 -> {
                            EdgeGestureDirection.PREV_PAGE
                        }
                        edgeReadyForNextPage && !recyclerView.canScrollVertically(1) -> {
                            EdgeGestureDirection.NEXT_PAGE
                        }
                        edgeReadyForPrevPage && !recyclerView.canScrollVertically(-1) -> {
                            EdgeGestureDirection.PREV_PAGE
                        }
                        else -> null
                    }
                    Log.d(
                        PAGING_LOG_TAG,
                        "ACTION_DOWN y=${event.y} dir=$edgeGestureDirection canDown=${recyclerView.canScrollVertically(1)} canUp=${recyclerView.canScrollVertically(-1)} page=${state.currentPage}/${state.totalPages} readyNext=$edgeReadyForNextPage readyPrev=$edgeReadyForPrevPage"
                    )
                }

                MotionEvent.ACTION_MOVE -> {
                    when (edgeGestureDirection) {
                        EdgeGestureDirection.NEXT_PAGE -> {
                            val distance = edgeGestureStartY - event.y
                            if (distance >= edgeTriggerThresholdPx && !edgeGestureArmed) {
                                edgeGestureArmed = true
                                Log.d(
                                    PAGING_LOG_TAG,
                                    "ACTION_MOVE arm NEXT distance=$distance threshold=$edgeTriggerThresholdPx"
                                )
                                showPagingHint(getString(R.string.home_hint_release_next))
                            }
                        }
                        EdgeGestureDirection.PREV_PAGE -> {
                            val distance = event.y - edgeGestureStartY
                            if (distance >= edgeTriggerThresholdPx && !edgeGestureArmed) {
                                edgeGestureArmed = true
                                Log.d(
                                    PAGING_LOG_TAG,
                                    "ACTION_MOVE arm PREV distance=$distance threshold=$edgeTriggerThresholdPx"
                                )
                                showPagingHint(getString(R.string.home_hint_release_prev))
                            }
                        }
                        null -> Unit
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    Log.d(
                        PAGING_LOG_TAG,
                        "ACTION_UP/CANCEL armed=$edgeGestureArmed dir=$edgeGestureDirection autoNext=$autoNextPagingTriggered autoPrev=$autoPrevPagingTriggered"
                    )
                    if (edgeGestureArmed) {
                        when (edgeGestureDirection) {
                            EdgeGestureDirection.NEXT_PAGE -> {
                                val state = homeViewModel.uiState.value
                                if (state.galleryListState is LoadState.Content &&
                                    state.currentPage < state.totalPages &&
                                    !autoNextPagingTriggered
                                ) {
                                    autoNextPagingTriggered = true
                                    edgeReadyForNextPage = false
                                    Log.d(
                                        PAGING_LOG_TAG,
                                        "trigger NEXT page from ${state.currentPage} to ${state.currentPage + 1}"
                                    )
                                    requestPage(state.currentPage + 1)
                                } else {
                                    Log.d(
                                        PAGING_LOG_TAG,
                                        "skip NEXT trigger content=${state.galleryListState is LoadState.Content} page=${state.currentPage}/${state.totalPages} autoNext=$autoNextPagingTriggered"
                                    )
                                }
                            }

                            EdgeGestureDirection.PREV_PAGE -> {
                                val loaded = tryLoadPrevPage(
                                    state = homeViewModel.uiState.value,
                                    requireAtTop = false
                                )
                                if (loaded) {
                                    edgeReadyForPrevPage = false
                                    Log.d(PAGING_LOG_TAG, "trigger PREV page")
                                } else {
                                    Log.d(PAGING_LOG_TAG, "skip PREV trigger")
                                }
                            }

                            null -> Unit
                        }
                    }
                    edgeGestureDirection = null
                    edgeGestureArmed = false
                }
            }
            false
        }
    }

    private fun tryLoadPrevPage(
        state: com.nhviewer.ui.home.HomeUiState,
        requireAtTop: Boolean
    ): Boolean {
        if (state.galleryListState !is LoadState.Content) {
            Log.d(PAGING_LOG_TAG, "tryLoadPrevPage blocked: state not content")
            return false
        }
        if (state.currentPage <= 1) {
            Log.d(PAGING_LOG_TAG, "tryLoadPrevPage blocked: already first page")
            return false
        }
        if (autoPrevPagingTriggered) {
            Log.d(PAGING_LOG_TAG, "tryLoadPrevPage blocked: autoPrevPagingTriggered=true")
            return false
        }
        if (requireAtTop && recyclerView.canScrollVertically(-1)) {
            Log.d(PAGING_LOG_TAG, "tryLoadPrevPage blocked: requireAtTop but can scroll up")
            return false
        }
        autoPrevPagingTriggered = true
        requestPage(state.currentPage - 1)
        return true
    }

    private fun showPagingHint(text: String) {
        edgeHintBar.text = text
        edgeHintBar.visibility = View.VISIBLE
        uiHandler.removeCallbacks(hideEdgeHintRunnable)
        uiHandler.postDelayed(hideEdgeHintRunnable, 1400L)
    }

    private fun showPagingFooter(state: PagingFooterState, autoHideMs: Long? = null) {
        pagingFooterState = state
        uiHandler.removeCallbacks(hidePagingFooterRunnable)
        when (state) {
            PagingFooterState.HIDDEN -> {
                pagingFooterBar.visibility = View.GONE
                pagingFooterProgress.visibility = View.GONE
            }

            PagingFooterState.LOADING -> {
                pagingFooterBar.visibility = View.VISIBLE
                pagingFooterProgress.visibility = View.VISIBLE
                pagingFooterText.text = getString(R.string.home_footer_loading)
            }

            PagingFooterState.RETRY -> {
                pagingFooterBar.visibility = View.VISIBLE
                pagingFooterProgress.visibility = View.GONE
                pagingFooterText.text = getString(R.string.home_footer_retry)
            }

            PagingFooterState.END -> {
                pagingFooterBar.visibility = View.VISIBLE
                pagingFooterProgress.visibility = View.GONE
                pagingFooterText.text = getString(R.string.home_footer_no_more)
            }
        }
        if (autoHideMs != null && state != PagingFooterState.HIDDEN) {
            uiHandler.postDelayed(hidePagingFooterRunnable, autoHideMs)
        }
    }

    private fun requestPage(page: Int) {
        Log.d(
            PAGING_LOG_TAG,
            "requestPage($page) from current=${homeViewModel.uiState.value.currentPage}/${homeViewModel.uiState.value.totalPages}"
        )
        pendingPageRequest = page
        homeViewModel.savedScrollIndex = 0
        homeViewModel.savedScrollOffset = 0
        homeViewModel.loadHome(page)
    }

    private fun showJumpToPageDialog() {
        val state = homeViewModel.uiState.value
        val totalPages = state.totalPages.coerceAtLeast(1)
        if (totalPages <= 1) {
            showPagingHint(getString(R.string.home_jump_single_page))
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.home_jump_hint, totalPages)
            setText(state.currentPage.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.home_jump_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val page = input.text?.toString()?.trim()?.toIntOrNull()
                if (page == null || page !in 1..totalPages) {
                    Toast.makeText(
                        this,
                        getString(R.string.home_jump_invalid, totalPages),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (page != state.currentPage) {
                    requestPage(page)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    renderQuickFilters(state.languageFilter, state.sortOption, state.hideBlacklisted)
                    val pageText = getString(
                        R.string.home_page_info,
                        state.currentPage,
                        state.totalPages.coerceAtLeast(1)
                    )
                    pageInfoView.text = pageText
                    prevPageButton.isEnabled = state.currentPage > 1
                    nextPageButton.isEnabled = state.currentPage < state.totalPages
                    if (state.galleryListState !is LoadState.Loading) {
                        autoNextPagingTriggered = false
                        autoPrevPagingTriggered = false
                    }
                    render(
                        state = state.galleryListState,
                        currentPage = state.currentPage,
                        totalPages = state.totalPages
                    )
                }
            }
        }
    }

    private fun cycleLanguageFilter() {
        val current = homeViewModel.uiState.value.languageFilter
        val next = when (current) {
            HomeLanguageFilter.ALL -> HomeLanguageFilter.JAPANESE
            HomeLanguageFilter.JAPANESE -> HomeLanguageFilter.CHINESE
            HomeLanguageFilter.CHINESE -> HomeLanguageFilter.ALL
        }
        homeViewModel.applySettingsFilters(
            languageFilter = next,
            sortOption = homeViewModel.uiState.value.sortOption,
            hideBlacklisted = homeViewModel.uiState.value.hideBlacklisted
        )
        homeViewModel.persistHomeLanguageFilter(next)
    }

    private fun cycleSortOption() {
        val current = homeViewModel.uiState.value.sortOption
        val next = when (current) {
            SortOption.RECENT -> SortOption.POPULAR
            SortOption.POPULAR -> SortOption.RANDOM
            SortOption.RANDOM -> SortOption.RECENT
        }
        homeViewModel.applySettingsFilters(
            languageFilter = homeViewModel.uiState.value.languageFilter,
            sortOption = next,
            hideBlacklisted = homeViewModel.uiState.value.hideBlacklisted
        )
        homeViewModel.persistHomeSortOption(next)
    }

    private fun toggleBlacklistFilter() {
        val next = !homeViewModel.uiState.value.hideBlacklisted
        homeViewModel.applySettingsFilters(
            languageFilter = homeViewModel.uiState.value.languageFilter,
            sortOption = homeViewModel.uiState.value.sortOption,
            hideBlacklisted = next
        )
        homeViewModel.persistHideBlacklisted(next)
    }

    private fun renderQuickFilters(
        languageFilter: HomeLanguageFilter,
        sortOption: SortOption,
        hideBlacklisted: Boolean
    ) {
        val languageText = when (languageFilter) {
            HomeLanguageFilter.ALL -> getString(R.string.home_language_all)
            HomeLanguageFilter.JAPANESE -> getString(R.string.home_language_japanese)
            HomeLanguageFilter.CHINESE -> getString(R.string.home_language_chinese)
        }
        val sortText = when (sortOption) {
            SortOption.RECENT -> getString(R.string.home_sort_recent)
            SortOption.POPULAR -> getString(R.string.home_sort_popular)
            SortOption.RANDOM -> getString(R.string.home_sort_random)
        }

        quickLanguageButton.text = getString(R.string.home_quick_language_template, languageText)
        quickSortButton.text = getString(R.string.home_quick_sort_template, sortText)
        quickBlacklistButton.text = if (hideBlacklisted) {
            getString(R.string.home_quick_blacklist_on)
        } else {
            getString(R.string.home_quick_blacklist_off)
        }
    }

    private fun collectSettingsAndFilters() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.settings.collect { settings ->
                    adapter.setPreferJapaneseTitle(settings.preferJapaneseTitle)
                    adapter.setShowChineseTag(settings.showChineseTags)
                    val languageFilter = when (settings.homeLanguageFilter.lowercase()) {
                        "japanese" -> HomeLanguageFilter.JAPANESE
                        "chinese" -> HomeLanguageFilter.CHINESE
                        else -> HomeLanguageFilter.ALL
                    }
                    val sortOption = when (settings.homeSortOption.lowercase()) {
                        "popular" -> SortOption.POPULAR
                        "random" -> SortOption.RANDOM
                        else -> SortOption.RECENT
                    }

                    if (!filterAppliedFromSettings) {
                        filterAppliedFromSettings = true
                        homeViewModel.applySettingsFilters(
                            languageFilter = languageFilter,
                            sortOption = sortOption,
                            hideBlacklisted = settings.hideBlacklisted,
                            loadIfChanged = false
                        )
                        requestPage(1)
                    } else {
                        homeViewModel.applySettingsFilters(
                            languageFilter = languageFilter,
                            sortOption = sortOption,
                            hideBlacklisted = settings.hideBlacklisted
                        )
                    }
                }
            }
        }
    }

    private fun render(
        state: LoadState<List<GallerySummary>>,
        currentPage: Int,
        totalPages: Int
    ) {
        when (state) {
            LoadState.Loading -> {
                val hasCurrentList = adapter.itemCount > 0
                recyclerView.visibility = if (hasCurrentList) View.VISIBLE else View.GONE
                messageView.visibility = View.GONE
                messageView.setOnClickListener(null)
                initialLoadingView.visibility = if (hasCurrentList) View.GONE else View.VISIBLE
                if (hasCurrentList) {
                    showPagingFooter(PagingFooterState.LOADING)
                } else {
                    showPagingFooter(PagingFooterState.HIDDEN)
                }
            }

            is LoadState.Content -> {
                pendingPageRequest = null
                recyclerView.visibility = View.VISIBLE
                messageView.visibility = View.GONE
                initialLoadingView.visibility = View.GONE
                messageView.setOnClickListener(null)
                adapter.submitList(state.value) {
                    val savedIndex = homeViewModel.savedScrollIndex
                    if (savedIndex > 0) {
                        (recyclerView.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(savedIndex, homeViewModel.savedScrollOffset)
                    }
                }
                showPagingFooter(PagingFooterState.HIDDEN)
            }

            LoadState.Empty -> {
                pendingPageRequest = null
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                initialLoadingView.visibility = View.GONE
                messageView.text = getString(R.string.home_empty_message)
                messageView.setOnClickListener(null)
                if (currentPage >= totalPages) {
                    showPagingFooter(PagingFooterState.END, autoHideMs = 1400L)
                } else {
                    showPagingFooter(PagingFooterState.HIDDEN)
                }
            }

            is LoadState.Error -> {
                initialLoadingView.visibility = View.GONE
                val hasCurrentList = adapter.itemCount > 0
                if (hasCurrentList) {
                    recyclerView.visibility = View.VISIBLE
                    messageView.visibility = View.GONE
                    messageView.setOnClickListener(null)
                    showPagingFooter(PagingFooterState.RETRY)
                } else {
                    recyclerView.visibility = View.GONE
                    messageView.visibility = View.VISIBLE
                    messageView.text = getString(R.string.home_error_message, state.message)
                    messageView.setOnClickListener {
                        val target = pendingPageRequest ?: currentPage
                        requestPage(target)
                    }
                    showPagingFooter(PagingFooterState.HIDDEN)
                }
            }
        }
    }
}
