package com.nhviewer.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhviewer.R
import com.nhviewer.app.AppGraph
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.SortOption
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.common.NhViewModelFactory
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

class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels { NhViewModelFactory() }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageView: TextView
    private lateinit var pageInfoView: TextView
    private lateinit var searchButton: Button
    private lateinit var favoritesButton: Button
    private lateinit var historyButton: Button
    private lateinit var settingsButton: Button
    private lateinit var downloadsButton: Button
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button

    private lateinit var adapter: HomeGalleryAdapter
    private var filterAppliedFromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        messageView = findViewById(R.id.messageView)
        pageInfoView = findViewById(R.id.pageInfoView)
        searchButton = findViewById(R.id.searchButton)
        favoritesButton = findViewById(R.id.favoritesButton)
        historyButton = findViewById(R.id.historyButton)
        settingsButton = findViewById(R.id.settingsButton)
        downloadsButton = findViewById(R.id.downloadsButton)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)

        adapter = HomeGalleryAdapter { item ->
            startActivity(
                Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
                }
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        searchButton.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        favoritesButton.setOnClickListener { startActivity(Intent(this, FavoritesActivity::class.java)) }
        historyButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        downloadsButton.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        prevPageButton.setOnClickListener {
            val current = homeViewModel.uiState.value.currentPage
            if (current > 1) homeViewModel.loadHome(current - 1)
        }
        nextPageButton.setOnClickListener {
            val current = homeViewModel.uiState.value.currentPage
            val total = homeViewModel.uiState.value.totalPages
            if (current < total) homeViewModel.loadHome(current + 1)
        }

        collectState()
        collectSettingsAndFilters()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    pageInfoView.text = getString(
                        R.string.home_page_info,
                        state.currentPage,
                        state.totalPages.coerceAtLeast(1)
                    )
                    prevPageButton.isEnabled = state.currentPage > 1
                    nextPageButton.isEnabled = state.currentPage < state.totalPages
                    render(state.galleryListState)
                }
            }
        }
    }

    private fun collectSettingsAndFilters() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settingsRepository.observeSettings().collect { settings ->
                    adapter.setPreferJapaneseTitle(settings.preferJapaneseTitle)

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

                    homeViewModel.setLanguageFilter(languageFilter)
                    homeViewModel.setSortOption(sortOption)
                    homeViewModel.setHideBlacklisted(settings.hideBlacklisted)

                    if (!filterAppliedFromSettings) {
                        filterAppliedFromSettings = true
                        homeViewModel.loadHome()
                    }
                }
            }
        }
    }

    private fun render(state: LoadState<List<GallerySummary>>) {
        when (state) {
            LoadState.Loading -> {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.GONE
                messageView.setOnClickListener(null)
            }

            is LoadState.Content -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                messageView.visibility = View.GONE
                messageView.setOnClickListener(null)
                adapter.submitList(state.value)
            }

            LoadState.Empty -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.home_empty_message)
                messageView.setOnClickListener(null)
            }

            is LoadState.Error -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.home_error_message, state.message)
                messageView.setOnClickListener { homeViewModel.loadHome() }
            }
        }
    }
}
