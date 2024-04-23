package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.LinkTubeActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.changeTab
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import java.util.Locale

class HistoryAdapter(
    private val historyItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener,
    private val isHomeFragment: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(historyItem: HistoryItem)
    }
    private var filteredItems: MutableList<HistoryItem> = mutableListOf()

    init {
        filteredItems.addAll(historyItems)
    }


    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_HOME = 1
    }

    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.TopicName)
        private val urlTextView: TextView = itemView.findViewById(R.id.websiteName)
        private val websiteImageView: ImageView = itemView.findViewById(R.id.iconWebsiteView)
        private val corsDeleteButton: ImageView = itemView.findViewById(R.id.corsDelete)

        fun bind(historyItem: HistoryItem) {
            titleTextView.text = historyItem.url
            urlTextView.text = historyItem.title
            if (historyItem.imageBitmap != null) {
                websiteImageView.setImageBitmap(historyItem.imageBitmap)
            } else {
                websiteImageView.setImageResource(R.drawable.search_link_tube)
            }

            corsDeleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    historyItems.removeAt(position)
                    notifyItemRemoved(position)
                    HistoryManager.deleteHistoryItem(historyItem, itemView.context)
                }
            }

            itemView.setOnClickListener {
                itemClickListener.onItemClick(historyItem)
            }
        }
    }

    inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyTitleTextView: TextView = itemView.findViewById(R.id.historyTitle)
        private val historyViewImageButton: ImageButton = itemView.findViewById(R.id.textFillArrow)

        fun bind(historyItem: HistoryItem) {
            historyTitleTextView.text = historyItem.url

            itemView.setOnClickListener {
                // Call method to navigate to browser fragment
                navigateToBrowserFragment(historyItem.url)

                // Hide the keyboard and update historyRecycler visibility
                val context = itemView.context
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                // Check if the keyboard is currently visible
                if (inputMethodManager.isAcceptingText) {
                    // Hide the keyboard
                    inputMethodManager.hideSoftInputFromWindow(itemView.windowToken, 0)

                    // Update historyRecycler visibility to GONE
                    val rootView = itemView.rootView
                    val historyRecycler = rootView.findViewById<RecyclerView>(R.id.historyRecycler)
                    historyRecycler.visibility = View.GONE
                }
            }

            historyViewImageButton.setOnClickListener {
                val linkTubeRef = itemView.context as LinkTubeActivity
                linkTubeRef.binding.btnTextUrl.setText(historyItem.url)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HOME -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_browser, parent, false)
                HomeViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_layout, parent, false)
                NormalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val historyItem = filteredItems[position]
        when (holder) {
            is NormalViewHolder -> holder.bind(historyItem)
            is HomeViewHolder -> holder.bind(historyItem)
        }
    }

    override fun getItemCount(): Int {
        return filteredItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHomeFragment) {
            VIEW_TYPE_HOME
        } else {
            VIEW_TYPE_NORMAL
        }
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
    private fun navigateToBrowserFragment(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }
}
