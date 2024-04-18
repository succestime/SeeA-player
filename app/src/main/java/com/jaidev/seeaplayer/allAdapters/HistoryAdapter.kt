package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import java.util.Locale

class HistoryAdapter(
    private val historyItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(historyItem: HistoryItem)
    }
    private var filteredItems: MutableList<HistoryItem> = mutableListOf()

    init {
        filteredItems.addAll(historyItems)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.TopicName)
        private val urlTextView: TextView = itemView.findViewById(R.id.websiteName)
        private val websiteImageView: ImageView = itemView.findViewById(R.id.iconWebsiteView)
        private val corsDeleteButton: ImageView = itemView.findViewById(R.id.corsDelete)

        fun bind(historyItem: HistoryItem) {
            titleTextView.text = historyItem.url
            urlTextView.text = historyItem.title
            // Ensure imageBitmap is not null before setting it
            if (historyItem.imageBitmap != null) {
                websiteImageView.setImageBitmap(historyItem.imageBitmap)
            } else {
                // Set a default placeholder image
                websiteImageView.setImageResource(R.drawable.search_link_tube)
            }
            // Set click listener for corsDeleteButton
            corsDeleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Remove item from filteredItems list and notify adapter
                    filteredItems.removeAt(position)
                    notifyItemRemoved(position)

                    // Delete history item from shared preferences
                    HistoryManager.deleteHistoryItem(historyItem, itemView.context)
                }
            }

            itemView.setOnClickListener {
                itemClickListener.onItemClick(historyItem)
            }
        }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_layout, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = filteredItems[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int {
        return filteredItems.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        historyItems.clear()
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun filterItems(searchText: String) {
        filteredItems.clear()
        if (searchText.isEmpty()) {
            filteredItems.addAll(historyItems)
        } else {
            val searchPattern = searchText.toLowerCase(Locale.getDefault())
            filteredItems.addAll(historyItems.filter {
                it.url.toLowerCase(Locale.getDefault()).contains(searchPattern)
            })
        }
        notifyDataSetChanged()
    }
}



