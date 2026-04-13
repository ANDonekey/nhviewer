package com.nhviewer.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.nhviewer.domain.model.Tag
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.common.NhViewModelFactory
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private val viewModel: SearchViewModel by viewModels { NhViewModelFactory() }

    private lateinit var queryInput: EditText
    private lateinit var tagInput: EditText
    private lateinit var searchButton: Button
    private lateinit var addTagButton: Button
    private lateinit var clearTagButton: Button
    private lateinit var selectedTagsView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageView: TextView
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageInfoView: TextView
    private lateinit var adapter: HomeGalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        queryInput = findViewById(R.id.queryInput)
        tagInput = findViewById(R.id.tagInput)
        searchButton = findViewById(R.id.searchButton)
        addTagButton = findViewById(R.id.addTagButton)
        clearTagButton = findViewById(R.id.clearTagButton)
        selectedTagsView = findViewById(R.id.selectedTagsView)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        messageView = findViewById(R.id.messageView)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageInfoView = findViewById(R.id.pageInfoView)

        adapter = HomeGalleryAdapter { item ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
            })
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchButton.setOnClickListener {
            val keyword = queryInput.text?.toString().orEmpty().trim()
            viewModel.updateKeyword(keyword)
            viewModel.search(1)
        }
        addTagButton.setOnClickListener {
            val keyword = tagInput.text?.toString().orEmpty()
            viewModel.addTagByKeyword(keyword)
        }
        clearTagButton.setOnClickListener {
            viewModel.clearTags()
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
        val selectedTagsText = if (state.queryState.selectedTags.isEmpty()) {
            getString(R.string.search_no_tags_selected)
        } else {
            state.queryState.selectedTags.joinToString("  ") { "${it.type}:${it.name}" }
        }
        selectedTagsView.text = getString(R.string.search_selected_tags_template, selectedTagsText)
        prevPageButton.isEnabled = currentPage > 1
        nextPageButton.isEnabled = currentPage < totalPages

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
                adapter.submitList(resultState.value)
            }

            LoadState.Empty -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.home_empty_message)
            }

            is LoadState.Error -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.text = getString(R.string.home_error_message, resultState.message)
            }
        }

        if (!state.tagMessage.isNullOrBlank()) {
            messageView.visibility = View.VISIBLE
            messageView.text = state.tagMessage
        }
    }

    companion object {
        const val EXTRA_PRESELECT_TAG_ID = "extra_preselect_tag_id"
        const val EXTRA_PRESELECT_TAG_TYPE = "extra_preselect_tag_type"
        const val EXTRA_PRESELECT_TAG_NAME = "extra_preselect_tag_name"
        const val EXTRA_PRESELECT_TAG_SLUG = "extra_preselect_tag_slug"
    }
}
