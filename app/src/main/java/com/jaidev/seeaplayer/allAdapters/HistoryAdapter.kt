package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.HistoryBrowser
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import java.util.Locale

class HistoryAdapter(
    private val context: Context,
    var historyItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(historyItem: HistoryItem)


    }

    var filteredItems: MutableList<HistoryItem> = mutableListOf()
    companion object {
        private const val VIEW_TYPE_NORMAL = 0
    }
    init {
        filteredItems.addAll(historyItems)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredItems.clear()
        if (query.isEmpty()) {
            filteredItems.addAll(historyItems)
        } else {
            val filterPattern = query.toLowerCase(Locale.getDefault()).trim()
            historyItems.forEach { item ->
                val title = item.url.toLowerCase(Locale.getDefault())
                val words = filterPattern.split("\\s+".toRegex()) // Split the query into words
                val matchesAllWords = words.all { word -> title.contains(word) }
                if (matchesAllWords) {
                    filteredItems.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun getTopFiveRecentItems(): List<HistoryItem> {
        return historyItems.take(5)
    }

    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.TopicName)
        private val urlTextView: TextView = itemView.findViewById(R.id.websiteName)
        private val websiteImageView: ImageView = itemView.findViewById(R.id.iconWebsiteView)
        private val corsDeleteButton: ImageView = itemView.findViewById(R.id.corsDelete)

        fun bind(historyItem: HistoryItem) {
            val topicName = extractTopicName(historyItem.url)
            titleTextView.text = topicName

            urlTextView.text = historyItem.title

            if (historyItem.imageBitmap != null) {
                websiteImageView.setImageBitmap(historyItem.imageBitmap)
            } else {
                websiteImageView.setImageResource(R.drawable.search_link_tube)
            }

            corsDeleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    filteredItems.removeAt(position)
                    notifyItemRemoved(position)
                    HistoryManager.deleteHistoryItem(historyItem, itemView.context)
                    (context as HistoryBrowser).updateEmptyStateVisibility()
                }
            }

            itemView.setOnClickListener {
                itemClickListener.onItemClick(historyItem)
            }
        }
        private fun extractTopicName(url: String): String {
            // Decode the URL to handle special characters properly
            val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")

            // Find the index of "?q=" in the decoded URL
            val index = decodedUrl.indexOf("?q=")
            return if (index != -1) {
                // Extract the substring after "?q="
                val queryString = decodedUrl.substring(index + 3)

                // Remove any additional parameters like "#pi=1" or "#sbfbu=1"
                val topicName = queryString.split("&")[0].split("#")[0]

                // Return the topic name
                topicName
            } else {
                // Return the entire URL if "?q=" is not found
                decodedUrl
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_layout, parent, false)
                NormalViewHolder(view)
            }


            else -> {val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_layout, parent, false)
                NormalViewHolder(view)}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val historyItem = filteredItems[position]
        when (holder) {
            is NormalViewHolder -> holder.bind(historyItem)
        }
    }

    override fun getItemCount(): Int {
        return filteredItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_NORMAL

    }


}