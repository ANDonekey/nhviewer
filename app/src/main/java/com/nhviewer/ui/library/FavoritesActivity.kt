package com.nhviewer.ui.library

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
import com.nhviewer.ui.common.NhViewModelFactory
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {
    private val viewModel: FavoritesViewModel by viewModels { NhViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_list)

        val titleView: TextView = findViewById(R.id.titleView)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val emptyView: TextView = findViewById(R.id.emptyView)
        val actionButton: Button = findViewById(R.id.actionButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        titleView.text = getString(R.string.nav_favorites)
        actionButton.text = getString(R.string.action_refresh)
        actionButton.setOnClickListener { viewModel.refresh() }

        val adapter = HomeGalleryAdapter { item ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
            })
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    titleView.text = if (state.source == "online") {
                        getString(R.string.nav_favorites_online)
                    } else {
                        getString(R.string.nav_favorites_local)
                    }
                    actionButton.visibility = if (state.source == "online") View.VISIBLE else View.GONE
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
