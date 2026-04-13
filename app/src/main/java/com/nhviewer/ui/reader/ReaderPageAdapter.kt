package com.nhviewer.ui.reader

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

class ReaderPageAdapter : ListAdapter<PageImage, ReaderPageAdapter.ReaderPageViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderPageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reader_page, parent, false)
        return ReaderPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReaderPageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReaderPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val pageIndexView: TextView = itemView.findViewById(R.id.pageIndexView)

        fun bind(item: PageImage) {
            val index = if (item.index > 0) item.index else bindingAdapterPosition + 1
            pageIndexView.text = itemView.context.getString(R.string.detail_page_index, index)
            imageView.load(item.url) {
                crossfade(true)
                placeholder(ColorDrawable(0xFFD0D0D0.toInt()))
                error(ColorDrawable(0xFFD0D0D0.toInt()))
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PageImage>() {
        override fun areItemsTheSame(oldItem: PageImage, newItem: PageImage): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: PageImage, newItem: PageImage): Boolean {
            return oldItem == newItem
        }
    }
}
