package com.nhviewer.ui.detail

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nhviewer.R
import com.nhviewer.domain.model.PageImage

class PreviewImageAdapter : ListAdapter<PageImage, PreviewImageAdapter.PreviewViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_image, parent, false)
        return PreviewViewHolder(view)
    }

    class PreviewViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val previewImageView: ImageView = itemView.findViewById(R.id.previewImageView)
        private val indexView: TextView = itemView.findViewById(R.id.indexView)

        fun bind(item: PageImage, onItemClick: (PageImage, Int) -> Unit) {
            val displayIndex = if (item.index <= 0) bindingAdapterPosition + 1 else item.index
            indexView.text = itemView.context.getString(R.string.detail_page_index, displayIndex)
            previewImageView.load(item.thumbnailUrl ?: item.url) {
                crossfade(true)
                placeholder(ColorDrawable(0xFFD0D0D0.toInt()))
                error(ColorDrawable(0xFFD0D0D0.toInt()))
            }
            itemView.setOnClickListener {
                onItemClick(item, displayIndex)
            }
        }
    }

    private var onItemClick: (PageImage, Int) -> Unit = { _, _ -> }

    fun setOnItemClickListener(listener: (PageImage, Int) -> Unit) {
        onItemClick = listener
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    private object DiffCallback : DiffUtil.ItemCallback<PageImage>() {
        override fun areItemsTheSame(oldItem: PageImage, newItem: PageImage): Boolean {
            return oldItem.index == newItem.index && oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: PageImage, newItem: PageImage): Boolean {
            return oldItem == newItem
        }
    }
}
