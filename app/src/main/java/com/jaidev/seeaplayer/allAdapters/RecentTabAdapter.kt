package com.jaidev.seeaplayer.allAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.databinding.RecantTabViewLayoutBinding

class RecentTabAdapter(
    private val context: Context,
    var recentItems: MutableList<HistoryItem>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecentTabAdapter.MyHolder>() {

    private var maxVisibleItems = 6

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
        return maxVisibleItems.coerceAtMost(recentItems.size)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = recentItems[position]
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
            recentItems.removeAt(position)

            // Notify adapter about the removed item
            notifyItemRemoved(position)

            // If there are more items to show, increase the max visible items and notify the adapter
            if (recentItems.size >= 5) {
                maxVisibleItems++
                notifyItemInserted(maxVisibleItems - 1)
            } else {
                notifyItemRangeChanged(position, recentItems.size)
            }
        }

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


