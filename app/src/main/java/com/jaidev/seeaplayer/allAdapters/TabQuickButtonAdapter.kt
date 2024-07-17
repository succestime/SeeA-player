package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.databinding.Tab2ViewBinding

class TabQuickButtonAdapter(
    private val context: Context
) : RecyclerView.Adapter<TabQuickButtonAdapter.MyHolder>() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: TabQuickButtonAdapter? = null

        @SuppressLint("NotifyDataSetChanged")
        fun updateTabs() {
            instance?.notifyDataSetChanged()
        }
    }

    init {
        instance = this
    }

    class MyHolder(binding: Tab2ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val tab2Icon = binding.tab2Icon
        val root = binding.root
        val tabCard = binding.tabCard // Add reference to the CardView
//        val closeButton = binding.closeButton // Add reference to the close button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(Tab2ViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        val tab = LinkTubeActivity.tabsList[position]

        // Set the website icon if available
        if (tab.icon != null) {
            holder.tab2Icon.setImageBitmap(tab.icon)
        } else {
            // Optionally, set a placeholder or default icon
            holder.tab2Icon.setImageResource(R.drawable.internat_browser_white)
        }

        // Update the UI for the current tab and other tabs
        if (LinkTubeActivity.myPager.currentItem == position) {
            holder.tabCard.setBackgroundResource(R.drawable.tab_selected_background)

        } else {
            holder.tabCard.setBackgroundResource(R.drawable.tab_2_background)
        }

        holder.root.setOnClickListener {
                val currentFragment = LinkTubeActivity.tabsList[position].fragment
                if (currentFragment is BrowseFragment && !currentFragment.binding.webView.url.isNullOrEmpty()) {
                    reloadBrowserFragment(currentFragment)
                }

                LinkTubeActivity.myPager.currentItem = position

            notifyDataSetChanged()


            }

    }

    override fun getItemCount(): Int {
        return LinkTubeActivity.tabsList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs() {
        notifyDataSetChanged()
    }

    private fun reloadBrowserFragment(fragment: BrowseFragment) {
        fragment.binding.webView.reload()
        fragment.binding.swipeRefreshBrowser.isRefreshing = false
    }

    private fun showErrorToast() {
        Toast.makeText(context, "Error occurred, restart the app", Toast.LENGTH_LONG).show()
    }
}
