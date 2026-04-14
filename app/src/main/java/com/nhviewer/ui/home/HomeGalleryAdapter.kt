package com.nhviewer.ui.home

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.nhviewer.R
import com.nhviewer.domain.model.GallerySummary
import com.nhviewer.domain.model.Tag
import java.util.Locale

class HomeGalleryAdapter(
    private val onItemClick: (GallerySummary) -> Unit,
    private val onItemLongClick: (GallerySummary, View) -> Unit = { _, _ -> }
) : ListAdapter<GallerySummary, HomeGalleryAdapter.GalleryViewHolder>(DiffCallback) {

    private var preferJapaneseTitle: Boolean = false
    private var showChineseTag: Boolean = true
    private var selectionMode: Boolean = false
    private val selectedIds = linkedSetOf<Long>()

    fun setPreferJapaneseTitle(enabled: Boolean) {
        if (preferJapaneseTitle == enabled) return
        preferJapaneseTitle = enabled
        notifyDataSetChanged()
    }

    fun setShowChineseTag(enabled: Boolean) {
        if (showChineseTag == enabled) return
        showChineseTag = enabled
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        if (selectionMode == enabled) return
        selectionMode = enabled
        if (!enabled) selectedIds.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(itemId: Long) {
        if (!selectionMode) return
        if (!selectedIds.add(itemId)) {
            selectedIds.remove(itemId)
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        if (!selectionMode) return
        selectedIds.clear()
        selectedIds.addAll(currentList.map { it.id })
        notifyDataSetChanged()
    }

    fun selectedCount(): Int = selectedIds.size
    fun selectedIds(): Set<Long> = selectedIds.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_summary, parent, false)
        return GalleryViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            item = item,
            preferJapaneseTitle = preferJapaneseTitle,
            showChineseTag = showChineseTag,
            selectionMode = selectionMode,
            selected = selectedIds.contains(item.id)
        )
    }

    class GalleryViewHolder(
        itemView: View,
        private val onItemClick: (GallerySummary) -> Unit,
        private val onItemLongClick: (GallerySummary, View) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)
        private val uploaderView: TextView = itemView.findViewById(R.id.uploaderView)
        private val pagesView: TextView = itemView.findViewById(R.id.pagesView)
        private val tagsView: TextView = itemView.findViewById(R.id.tagsView)
        private val categoryView: TextView = itemView.findViewById(R.id.categoryView)
        private val coverView: android.widget.ImageView = itemView.findViewById(R.id.coverView)
        private val cardView: MaterialCardView = itemView as MaterialCardView

        fun bind(
            item: GallerySummary,
            preferJapaneseTitle: Boolean,
            showChineseTag: Boolean,
            selectionMode: Boolean,
            selected: Boolean
        ) {
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
            uploaderView.text = itemView.context.getString(R.string.gallery_item_subtitle, item.id)

            val languageKey = resolveLanguageKey(item.tags)

            pagesView.text = itemView.context.getString(
                R.string.gallery_item_flag_pages,
                languageFlag(languageKey),
                item.pageCount
            )

            val tags = item.tags
                .asSequence()
                .filterNot { tag ->
                    tag.type.equals("language", ignoreCase = true) ||
                        tag.type.equals("category", ignoreCase = true)
                }
                .map { displayTagName(it, showChineseTag) }
                .distinct()
                .take(4)
                .joinToString("  ")

            tagsView.text = if (tags.isBlank()) {
                itemView.context.getString(R.string.gallery_no_tags)
            } else {
                tags
            }

            categoryView.text = item.tags.firstOrNull {
                it.type.equals("category", ignoreCase = true)
            }?.let { displayTagName(it, showChineseTag).uppercase(Locale.getDefault()) }
                ?: itemView.context.getString(R.string.detail_category_default)

            if (selectionMode && selected) {
                cardView.strokeWidth = 2
                cardView.strokeColor = 0xFF26A69A.toInt()
                cardView.alpha = 0.85f
            } else {
                cardView.strokeWidth = 0
                cardView.alpha = 1f
            }

            coverView.load(item.coverUrl) {
                crossfade(true)
                placeholder(ColorDrawable(0xFFCCCCCC.toInt()))
                error(ColorDrawable(0xFFCCCCCC.toInt()))
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item, itemView)
                true
            }
        }

        private fun languageFlag(languageKey: String?): String {
            return when (languageKey) {
                "chinese" -> "\uD83C\uDDE8\uD83C\uDDF3"
                "japanese" -> "\uD83C\uDDEF\uD83C\uDDF5"
                "english" -> "\uD83C\uDDFA\uD83C\uDDF8"
                else -> "\uD83C\uDF10"
            }
        }

        private fun resolveLanguageKey(tags: List<Tag>): String? {
            val languageTags = tags.filter { it.type.equals("language", ignoreCase = true) }
            if (languageTags.isEmpty()) return null

            val candidates = languageTags.map {
                listOf(it.slug, it.name)
                    .firstOrNull { raw -> !raw.isNullOrBlank() }
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()
            }

            return when {
                candidates.any { it.contains("chinese") } -> "chinese"
                candidates.any { it.contains("japanese") } -> "japanese"
                candidates.any { it.contains("english") } -> "english"
                // translated is a marker and not a concrete language
                candidates.any { it.contains("translated") } -> "translated"
                else -> candidates.firstOrNull { it.isNotBlank() }
            }
        }

        private fun displayTagName(tag: Tag, showChineseTag: Boolean): String {
            if (showChineseTag) {
                val zh = tag.nameZh?.trim().orEmpty()
                if (zh.isNotEmpty()) return zh
            }
            return tag.name
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

