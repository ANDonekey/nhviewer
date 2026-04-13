package com.nhviewer.ui.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhviewer.R
import com.nhviewer.download.DownloadStatus
import com.nhviewer.download.DownloadTask

class DownloadTaskAdapter(
    private val onRetry: (DownloadTask) -> Unit,
    private val onCancel: (DownloadTask) -> Unit
) : ListAdapter<DownloadTask, DownloadTaskAdapter.TaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download_task, parent, false)
        return TaskViewHolder(view, onRetry, onCancel)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        itemView: View,
        private val onRetry: (DownloadTask) -> Unit,
        private val onCancel: (DownloadTask) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)
        private val statusView: TextView = itemView.findViewById(R.id.statusView)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val retryButton: Button = itemView.findViewById(R.id.retryButton)
        private val cancelButton: Button = itemView.findViewById(R.id.cancelButton)

        fun bind(item: DownloadTask) {
            titleView.text = item.title
            val progress = if (item.total > 0) (item.completed * 100 / item.total) else 0
            progressBar.progress = progress.coerceIn(0, 100)
            statusView.text = itemView.context.getString(
                R.string.download_status_template,
                item.status.name,
                item.completed,
                item.total,
                item.errorMessage ?: ""
            )

            retryButton.visibility = if (item.status == DownloadStatus.FAILED) View.VISIBLE else View.GONE
            retryButton.setOnClickListener { onRetry(item) }

            cancelButton.visibility = if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.QUEUED) {
                View.VISIBLE
            } else {
                View.GONE
            }
            cancelButton.setOnClickListener { onCancel(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
        override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem == newItem
        }
    }
}
