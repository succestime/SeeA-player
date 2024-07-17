package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.UniqueHistoryItem
import com.jaidev.seeaplayer.databinding.RecantTabViewLayoutBinding

class RecentTabAdapter(
    private val context: Context,
    var recentItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecentTabAdapter.MyHolder>() {

    private var maxVisibleItems = 6
    private var uniqueItems: MutableList<UniqueHistoryItem> = mutableListOf()

    init {
        updateUniqueItems()
    }

    class MyHolder(binding: RecantTabViewLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val tabTopicName = binding.TabTopicName
        val tabWebsiteIcon = binding.tabWebsiteIcon
        val tabWebsiteName = binding.tabWebsiteName
        val root = binding.root
    }

    interface ItemClickListener {
        fun onItemClick(historyItem: HistoryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(RecantTabViewLayoutBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun getItemCount(): Int {
        return maxVisibleItems.coerceAtMost(uniqueItems.size)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = uniqueItems[position].historyItem
        val topicName = extractTopicName(item.url)
        holder.tabTopicName.text = topicName

        if (item.imageBitmap != null) {
            holder.tabWebsiteIcon.setImageBitmap(item.imageBitmap)
        } else {
            holder.tabWebsiteIcon.setImageResource(R.drawable.search_link_tube)
        }

        holder.tabWebsiteName.text = item.title
        holder.root.setOnClickListener {
            // Handle item click event
            itemClickListener.onItemClick(item)

            // Remove the clicked item from the list
            recentItems.remove(item)

            // Update the unique items list
            updateUniqueItems()

            // Notify adapter about the removed item
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, uniqueItems.size)
        }
    }

    private fun updateUniqueItems() {
        val uniqueMap = mutableMapOf<String, UniqueHistoryItem>()

        for (item in recentItems) {
            val topicName = extractTopicName(item.url)
            if (uniqueMap.containsKey(topicName)) {
                uniqueMap[topicName]?.duplicateCount = uniqueMap[topicName]?.duplicateCount?.plus(1) ?: 1
            } else {
                uniqueMap[topicName] = UniqueHistoryItem(item, 0)
            }
        }

        uniqueItems = uniqueMap.values.toMutableList()
        notifyDataSetChanged()
    }

    private fun extractTopicName(url: String): String {
        val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
        val index = decodedUrl.indexOf("?q=")
        return if (index != -1) {
            val queryString = decodedUrl.substring(index + 3)
            val topicName = queryString.split("&")[0].split("#")[0]
            topicName
        } else {
            decodedUrl
        }
    }
}
