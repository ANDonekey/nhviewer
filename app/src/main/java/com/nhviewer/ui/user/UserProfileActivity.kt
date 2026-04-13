package com.nhviewer.ui.user

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
import coil.load
import com.nhviewer.R
import com.nhviewer.domain.model.UserProfile
import com.nhviewer.ui.common.LoadState
import com.nhviewer.ui.common.NhViewModelFactory
import com.nhviewer.ui.detail.DetailActivity
import com.nhviewer.ui.home.HomeGalleryAdapter
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {
    private val viewModel: UserProfileViewModel by viewModels { NhViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val avatarView: android.widget.ImageView = findViewById(R.id.avatarView)
        val usernameView: TextView = findViewById(R.id.usernameView)
        val aboutView: TextView = findViewById(R.id.aboutView)
        val tagsView: TextView = findViewById(R.id.favoriteTagsView)
        val recyclerView: RecyclerView = findViewById(R.id.recentFavoritesRecycler)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val messageView: TextView = findViewById(R.id.messageView)
        val retryButton: Button = findViewById(R.id.retryButton)

        val adapter = HomeGalleryAdapter { item ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_GALLERY_ID, item.id)
            })
        }
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recyclerView.adapter = adapter

        retryButton.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (val profileState = state.profileState) {
                        LoadState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            messageView.visibility = View.GONE
                            retryButton.visibility = View.GONE
                            recyclerView.visibility = View.GONE
                        }

                        is LoadState.Content -> {
                            renderProfile(
                                profile = profileState.value,
                                avatarView = avatarView,
                                usernameView = usernameView,
                                aboutView = aboutView,
                                tagsView = tagsView,
                                recyclerView = recyclerView,
                                adapter = adapter
                            )
                            progressBar.visibility = View.GONE
                            messageView.visibility = View.GONE
                            retryButton.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }

                        is LoadState.Error -> {
                            progressBar.visibility = View.GONE
                            messageView.visibility = View.VISIBLE
                            messageView.text = getString(R.string.user_profile_load_error, profileState.message)
                            retryButton.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        }

                        LoadState.Empty -> Unit
                    }
                }
            }
        }

        viewModel.load()
    }

    private fun renderProfile(
        profile: UserProfile,
        avatarView: android.widget.ImageView,
        usernameView: TextView,
        aboutView: TextView,
        tagsView: TextView,
        recyclerView: RecyclerView,
        adapter: HomeGalleryAdapter
    ) {
        avatarView.load(profile.avatarUrl)
        usernameView.text = getString(R.string.user_profile_username, profile.username, profile.id)
        aboutView.text = if (profile.about.isBlank()) {
            getString(R.string.user_profile_about_empty)
        } else {
            profile.about
        }
        tagsView.text = if (profile.favoriteTags.isBlank()) {
            getString(R.string.user_profile_tags_empty)
        } else {
            profile.favoriteTags
        }
        adapter.submitList(profile.recentFavorites)
        recyclerView.visibility = if (profile.recentFavorites.isEmpty()) View.GONE else View.VISIBLE
    }
}
