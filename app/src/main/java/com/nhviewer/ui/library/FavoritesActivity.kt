package com.nhviewer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhviewer.R
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoritesActivity : AppCompatActivity() {
    private val viewModel: FavoritesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_list)

        val titleView: TextView = findViewById(R.id.titleView)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val emptyView: TextView = findViewById(R.id.emptyView)
        val actionButton: Button = findViewById(R.id.actionButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val batchBar: View = findViewById(R.id.batchBar)
        val batchCountView: TextView = findViewById(R.id.batchCountView)
        val batchSelectAllButton: Button = findViewById(R.id.batchSelectAllButton)
        val batchDeleteButton: Button = findViewById(R.id.batchDeleteButton)
        val batchCancelButton: Button = findViewById(R.id.batchCancelButton)

        titleView.text = getString(R.string.nav_favorites)
        actionButton.text = getString(R.string.action_refresh)
        actionButton.setOnClickListener { viewModel.refresh() }

        var selectionMode = false
        lateinit var adapter: HomeGalleryAdapter
        adapter = HomeGalleryAdapter(
            onItemClick = { item ->
                if (selectionMode) {
                    adapter.toggleSelection(item.id)
                    batchCountView.text = getString(R.string.batch_selected_count, adapter.selectedCount())
                } else {
                    startActivity(Intent(this, DetailActivity::class.java).apply {
                        putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
                    })
                }
            },
            onItemLongClick = { item, _ ->
                if (!selectionMode) {
                    selectionMode = true
                    adapter.setSelectionMode(true)
                    batchBar.visibility = View.VISIBLE
                    actionButton.visibility = View.GONE
                }
                adapter.toggleSelection(item.id)
                batchCountView.text = getString(R.string.batch_selected_count, adapter.selectedCount())
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        batchSelectAllButton.setOnClickListener {
            adapter.selectAll()
            batchCountView.text = getString(R.string.batch_selected_count, adapter.selectedCount())
        }
        batchDeleteButton.setOnClickListener {
            viewModel.removeByIds(adapter.selectedIds())
            selectionMode = false
            adapter.setSelectionMode(false)
            batchBar.visibility = View.GONE
            actionButton.visibility = if (viewModel.uiState.value.source == "online") View.VISIBLE else View.GONE
        }
        batchCancelButton.setOnClickListener {
            selectionMode = false
            adapter.setSelectionMode(false)
            batchBar.visibility = View.GONE
            actionButton.visibility = if (viewModel.uiState.value.source == "online") View.VISIBLE else View.GONE
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    titleView.text = if (state.source == "online") {
                        getString(R.string.nav_favorites_online)
                    } else {
                        getString(R.string.nav_favorites_local)
                    }
                    actionButton.visibility = if (!selectionMode && state.source == "online") View.VISIBLE else View.GONE
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    adapter.submitList(state.list)
                    if (state.error != null) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = getString(R.string.favorites_load_error, state.error)
                    } else {
                        emptyView.visibility = if (state.list.isEmpty()) View.VISIBLE else View.GONE
                        emptyView.text = getString(R.string.empty_favorites)
                    }
                }
            }
        }
    }
}
