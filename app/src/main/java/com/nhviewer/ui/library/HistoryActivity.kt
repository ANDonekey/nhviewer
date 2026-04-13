package com.nhviewer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

class HistoryActivity : AppCompatActivity() {
    private val viewModel: HistoryViewModel by viewModels { NhViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_list)

        val titleView: TextView = findViewById(R.id.titleView)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val emptyView: TextView = findViewById(R.id.emptyView)
        val actionButton: Button = findViewById(R.id.actionButton)

        titleView.text = getString(R.string.nav_history)
        actionButton.visibility = View.VISIBLE
        actionButton.text = getString(R.string.clear_history)
        actionButton.setOnClickListener { viewModel.clearHistory() }

        val adapter = HomeGalleryAdapter { item ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
            })
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    emptyView.text = getString(R.string.empty_history)
                }
            }
        }
    }
}
