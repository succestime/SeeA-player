
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.TabActivity
import com.jaidev.seeaplayer.databinding.TabLayoutBinding

class TabAdapter(private val context: Context
): RecyclerView.Adapter<TabAdapter.MyHolder>() {


    class MyHolder(binding: TabLayoutBinding)
        :RecyclerView.ViewHolder(binding.root) {
        val cancelBtn = binding.cancelBtn
        val name = binding.tabName
        val websiteIcon = binding.websiteIcon
        val websiteView= binding.tabViewImage
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(TabLayoutBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val tab = LinkTubeActivity.tabsList[position]
        val googleSearchPrefix = "https://www.google.com/search?q="

        // Decode and clean the tab name if it starts with the Google search prefix
        holder.name.text = if (tab.name.startsWith(googleSearchPrefix)) {
            cleanTabName(Uri.decode(tab.name.removePrefix(googleSearchPrefix)))
        } else {
            cleanTabName(tab.name)
        }

// Set the website icon if available
        if (tab.icon != null) {
            holder.websiteIcon.setImageBitmap(tab.icon)
        } else {
            // Optionally, set a placeholder or default icon
            holder.websiteIcon.setImageResource(R.drawable.internat_browser_white)
        }

        // Set the preview bitmap to websiteView
        if (tab.previewBitmap != null) {
            holder.websiteView.setImageBitmap(tab.previewBitmap)
        } else {
            // Set a placeholder or default icon if the previewBitmap is not set
            holder.websiteView.setImageResource(R.drawable.tab_internat_browser_gray)
        }

        holder.root.setOnClickListener {
            val currentFragment = LinkTubeActivity.tabsList[position].fragment
            if (currentFragment is BrowseFragment && !currentFragment.binding.webView.url.isNullOrEmpty()) {
                reloadBrowserFragment(currentFragment)
            }
            LinkTubeActivity.myPager.currentItem = position
            (context as TabActivity).finish()
        }

        holder.cancelBtn.setOnClickListener{
                LinkTubeActivity.tabsList.removeAt(position)
                notifyDataSetChanged()
                LinkTubeActivity.myPager.adapter?.notifyItemRemoved(position)
            (context as TabActivity).updateEmptyViewVisibility()
        }
    }

    override fun getItemCount(): Int {
        return LinkTubeActivity.tabsList.size
    }
    private fun cleanTabName(name: String): String {
        // Replace special characters with empty strings
        return name.replace("[*,\$,# ,%]".toRegex(), "")
    }
    private fun reloadBrowserFragment(fragment: BrowseFragment) {
        fragment.binding.webView.reload()
        fragment.binding.swipeRefreshBrowser.isRefreshing = false
    }

}
