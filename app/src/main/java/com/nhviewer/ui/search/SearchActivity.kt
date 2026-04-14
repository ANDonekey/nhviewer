package com.nhviewer.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nhviewer.R
import com.nhviewer.domain.model.SortOption
import com.nhviewer.domain.model.Tag
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchActivity : AppCompatActivity() {
    private val viewModel: SearchViewModel by viewModel()

    private lateinit var queryInput: EditText
    private lateinit var tagInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var addTagButton: MaterialButton
    private lateinit var sortButton: MaterialButton
    private lateinit var languageButton: MaterialButton
    private lateinit var selectedTagsChipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageView: TextView
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageInfoView: TextView
    private lateinit var adapter: HomeGalleryAdapter

    private var autoPagingTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        queryInput = findViewById(R.id.queryInput)
        tagInput = findViewById(R.id.tagInput)
        searchButton = findViewById(R.id.searchButton)
        addTagButton = findViewById(R.id.addTagButton)
        sortButton = findViewById(R.id.sortButton)
        languageButton = findViewById(R.id.languageButton)
        selectedTagsChipGroup = findViewById(R.id.selectedTagsChipGroup)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        messageView = findViewById(R.id.messageView)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageInfoView = findViewById(R.id.pageInfoView)

        adapter = HomeGalleryAdapter(onItemClick = { item ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
            })
        })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupAutoPaging()

        searchButton.setOnClickListener { doSearchFromInput() }
        queryInput.setOnEditorActionListener { _, _, event ->
            if (event == null || event.keyCode == KeyEvent.KEYCODE_ENTER) {
                doSearchFromInput()
                true
            } else {
                false
            }
        }

        addTagButton.setOnClickListener {
            val keyword = tagInput.text?.toString().orEmpty()
            viewModel.addTagByKeyword(keyword)
            tagInput.setText("")
        }

        sortButton.setOnClickListener {
            val current = viewModel.uiState.value.queryState.sortOption
            val next = when (current) {
                SortOption.POPULAR -> SortOption.RECENT
                SortOption.RECENT -> SortOption.RANDOM
                SortOption.RANDOM -> SortOption.POPULAR
            }
            viewModel.updateSort(next)
            viewModel.search(1)
        }

        languageButton.setOnClickListener {
            val current = viewModel.uiState.value.queryState.languageFilter
            val next = when (current) {
                SearchLanguageFilter.ALL -> SearchLanguageFilter.JAPANESE
                SearchLanguageFilter.JAPANESE -> SearchLanguageFilter.CHINESE
                SearchLanguageFilter.CHINESE -> SearchLanguageFilter.ALL
            }
            viewModel.applyLanguageFilterAndSearch(next)
        }

        prevPageButton.setOnClickListener {
            val currentPage = viewModel.uiState.value.queryState.page
            if (currentPage > 1) viewModel.search(currentPage - 1)
        }
        nextPageButton.setOnClickListener {
            val currentPage = viewModel.uiState.value.queryState.page
            val total = viewModel.uiState.value.totalPages
            if (currentPage < total) viewModel.search(currentPage + 1)
        }

        if (savedInstanceState == null) {
            applyPreselectedTagFromIntent()
        }
        collectState()
    }

    private fun doSearchFromInput() {
        val keyword = queryInput.text?.toString().orEmpty().trim()
        viewModel.updateKeyword(keyword)
        viewModel.search(1)
    }

    private fun applyPreselectedTagFromIntent() {
        val tagId = intent.getLongExtra(EXTRA_PRESELECT_TAG_ID, -1L)
        if (tagId <= 0L) return
        val tagType = intent.getStringExtra(EXTRA_PRESELECT_TAG_TYPE).orEmpty().ifBlank { "tag" }
        val tagName = intent.getStringExtra(EXTRA_PRESELECT_TAG_NAME).orEmpty()
        val tagSlug = intent.getStringExtra(EXTRA_PRESELECT_TAG_SLUG).orEmpty()
        val preselectTag = Tag(
            id = tagId,
            type = tagType,
            name = tagName.ifBlank { tagSlug.ifBlank { "tag-$tagId" } },
            slug = tagSlug
        )
        viewModel.updateTags(listOf(preselectTag))
        viewModel.search(1)
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: SearchUiState) {
        val currentPage = state.queryState.page
        val totalPages = state.totalPages.coerceAtLeast(1)
        pageInfoView.text = getString(R.string.search_page_info, currentPage, totalPages)
        prevPageButton.isEnabled = currentPage > 1
        nextPageButton.isEnabled = currentPage < totalPages
        renderSortButton(state.queryState.sortOption)
        renderLanguageButton(state.queryState.languageFilter)
        renderTagChips(state.queryState.selectedTags)

        when (val resultState = state.resultState) {
            LoadState.Loading -> {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.GONE
            }
            is LoadState.Content -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                messageView.visibility = View.GONE
                autoPagingTriggered = false
                adapter.submitList(resultState.value)
            }
            LoadState.Empty -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                autoPagingTriggered = false
                messageView.text = getString(R.string.home_empty_message)
            }
            is LoadState.Error -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                autoPagingTriggered = false
                messageView.text = getString(R.string.home_error_message, resultState.message)
            }
        }

        if (!state.tagMessage.isNullOrBlank()) {
            messageView.visibility = View.VISIBLE
            messageView.text = state.tagMessage
        }
    }

    private fun renderSortButton(sort: SortOption) {
        sortButton.text = when (sort) {
            SortOption.POPULAR -> getString(R.string.search_sort_popular)
            SortOption.RECENT -> getString(R.string.search_sort_recent)
            SortOption.RANDOM -> getString(R.string.search_sort_random)
        }
    }

    private fun renderLanguageButton(filter: SearchLanguageFilter) {
        languageButton.text = when (filter) {
            SearchLanguageFilter.ALL -> getString(R.string.search_language_all)
            SearchLanguageFilter.JAPANESE -> getString(R.string.search_language_japanese)
            SearchLanguageFilter.CHINESE -> getString(R.string.search_language_chinese)
        }
    }

    private fun renderTagChips(tags: List<Tag>) {
        selectedTagsChipGroup.removeAllViews()
        if (tags.isEmpty()) {
            val chip = Chip(this).apply {
                text = getString(R.string.search_no_tags_selected)
                isCheckable = false
                isClickable = false
            }
            selectedTagsChipGroup.addView(chip)
            return
        }
        tags.forEach { tag ->
            val chip = Chip(this).apply {
                text = "${tag.type}:${tag.name}"
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    viewModel.removeTag(tag.id)
                    viewModel.search(1)
                }
            }
            selectedTagsChipGroup.addView(chip)
        }
    }

    private fun setupAutoPaging() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val state = viewModel.uiState.value
                if (state.resultState !is LoadState.Content) return
                if (state.queryState.page >= state.totalPages) return
                if (autoPagingTriggered) return
                if (recyclerView.canScrollVertically(1)) return

                autoPagingTriggered = true
                viewModel.search(state.queryState.page + 1)
            }
        })
    }

    companion object {
        const val EXTRA_PRESELECT_TAG_ID = "extra_preselect_tag_id"
        const val EXTRA_PRESELECT_TAG_TYPE = "extra_preselect_tag_type"
        const val EXTRA_PRESELECT_TAG_NAME = "extra_preselect_tag_name"
        const val EXTRA_PRESELECT_TAG_SLUG = "extra_preselect_tag_slug"
    }
}
