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
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager

class HistoryAdapter(
    private var historyItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener,
    private val isHomeFragment: Boolean = false , private val isBrowseFragment: Boolean = false
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
            // Extract the topic name from the URL
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
                    historyItems.removeAt(position)
                    notifyItemRemoved(position)
                    HistoryManager.deleteHistoryItem(historyItem, itemView.context)
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
        inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyTitleTextView: TextView = itemView.findViewById(R.id.historyTitle)
        private val historyViewImageButton: ImageButton = itemView.findViewById(R.id.textFillArrow)

        fun bind(historyItem: HistoryItem) {
            val topicName = extractTopicName(historyItem.url)

            historyTitleTextView.text = topicName

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
                    historyRecycler.visibility = View.VISIBLE
                }
            }

            historyViewImageButton.setOnClickListener {
                val linkTubeRef = itemView.context as LinkTubeActivity
                linkTubeRef.binding.btnTextUrl.setText(historyItem.url)
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
            VIEW_TYPE_HOME -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_browser, parent, false)
                HomeViewHolder(view)
            }
            else -> {
                // Check if it's a browser fragment and inflate the appropriate layout
                if (isBrowseFragment) {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_browser, parent, false)
                    HomeViewHolder(view)
                } else {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.history_view_layout, parent, false)
                    NormalViewHolder(view)
                }
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val historyItem = historyItems[position]
        when (holder) {
            is NormalViewHolder -> holder.bind(historyItem)
            is HomeViewHolder -> holder.bind(historyItem)
        }
    }

    override fun getItemCount(): Int {
        return historyItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHomeFragment) {
            VIEW_TYPE_HOME
        } else if (isBrowseFragment) {
            VIEW_TYPE_HOME // Use the same view type for browser fragment if it's a browser fragment
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
    fun filterList(filteredList: List<HistoryItem>) {
        historyItems = ArrayList()
        historyItems.addAll(filteredList)
        notifyDataSetChanged()
    }


    private fun navigateToBrowserFragment(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }



}
