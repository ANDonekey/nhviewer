package com.nhviewer.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nhviewer.R
import com.nhviewer.domain.model.GallerySummary

class HomeGalleryAdapter(
    private val onItemClick: (GallerySummary) -> Unit
) : ListAdapter<GallerySummary, HomeGalleryAdapter.GalleryViewHolder>(DiffCallback) {

    private var preferJapaneseTitle: Boolean = false

    fun setPreferJapaneseTitle(enabled: Boolean) {
        if (preferJapaneseTitle == enabled) return
        preferJapaneseTitle = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_summary, parent, false)
        return GalleryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(getItem(position), preferJapaneseTitle)
    }

    class GalleryViewHolder(
        itemView: View,
        private val onItemClick: (GallerySummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)
        private val pagesView: TextView = itemView.findViewById(R.id.pagesView)
        private val tagsView: TextView = itemView.findViewById(R.id.tagsView)
        private val coverView: android.widget.ImageView = itemView.findViewById(R.id.coverView)

        fun bind(item: GallerySummary, preferJapaneseTitle: Boolean) {
            val preferredTitle = if (preferJapaneseTitle) {
                item.japaneseTitle?.takeIf { it.isNotBlank() }
                    ?: item.englishTitle?.takeIf { it.isNotBlank() }
                    ?: item.title
            } else {
                item.englishTitle?.takeIf { it.isNotBlank() }
                    ?: item.japaneseTitle?.takeIf { it.isNotBlank() }
                    ?: item.title
            }
            titleView.text = preferredTitle.ifBlank {
                itemView.context.getString(R.string.gallery_untitled, item.id)
            }
            pagesView.text = itemView.context.getString(R.string.gallery_pages, item.pageCount)
            val tags = item.tags.take(5).joinToString("  ") { "#${it.name}" }
            tagsView.text = if (tags.isBlank()) {
                itemView.context.getString(R.string.gallery_no_tags)
            } else {
                tags
            }

            coverView.load(item.coverUrl) {
                crossfade(true)
                placeholder(ColorDrawable(0xFFCCCCCC.toInt()))
                error(ColorDrawable(0xFFCCCCCC.toInt()))
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GallerySummary>() {
        override fun areItemsTheSame(oldItem: GallerySummary, newItem: GallerySummary): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GallerySummary, newItem: GallerySummary): Boolean {
            return oldItem == newItem
        }
    }
}
