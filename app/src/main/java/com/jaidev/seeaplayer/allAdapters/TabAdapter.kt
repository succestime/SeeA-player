package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.TabActivity
import com.jaidev.seeaplayer.dataClass.Tab

class TabAdapter(private val context: Context, private val dialog: AlertDialog?
                 , private var actionMode: ActionMode? = null , private val isLinktubeActivity : Boolean = false

                 ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: TabAdapter? = null

        @SuppressLint("NotifyDataSetChanged")
        fun updateTabs() {
            instance?.notifyDataSetChanged()
        }
    }

    init {
        instance = this
    }

    private var isSelectionMode = false
    val selectedItems = mutableSetOf<Int>()
    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cancelBtn: ImageView = itemView.findViewById(R.id.cancelBtn)
        private val name: TextView = itemView.findViewById(R.id.tabName)
        private val websiteIcon: ShapeableImageView = itemView.findViewById(R.id.websiteIcon)
        private val websiteView: ShapeableImageView = itemView.findViewById(R.id.tabViewImage)
        private val selectBtn: ImageView = itemView.findViewById(R.id.selectBtn)
        private val completeBtn: ImageView = itemView.findViewById(R.id.completeBtn)

        @SuppressLint("NotifyDataSetChanged")
        fun bind(fileItem: Tab) {
            try {

                if (position == 0) {
                    // Always set the properties for the first tab
                   websiteIcon.setImageResource(R.drawable.internat_browser_white)
                   websiteView.setImageResource(R.drawable.tab_internat_browser_gray)
                    name.text = "Home"
                } else {
                    val tab = LinkTubeActivity.tabsList[position]
                    val googleSearchPrefix = "https://www.google.com/search?q="

                    // Decode and clean the tab name if it starts with the Google search prefix
                   name.text = if (tab.name.startsWith(googleSearchPrefix)) {
                        cleanTabName(Uri.decode(tab.name.removePrefix(googleSearchPrefix)))
                    } else {
                        cleanTabName(tab.name)
                    }

                    // Set the website icon if available
                    if (tab.icon != null) {
                        websiteIcon.setImageBitmap(tab.icon)
                    } else {
                        // Optionally, set a placeholder or default icon
                       websiteIcon.setImageResource(R.drawable.internat_browser_white)
                    }

                    // Set the preview bitmap to websiteView
                    if (tab.previewBitmap != null) {
                       websiteView.setImageBitmap(tab.previewBitmap)
                    } else {
                        // Set a placeholder or default icon if the previewBitmap is not set
                      websiteView.setImageResource(R.drawable.tab_internat_browser_gray)
                    }
                }
                if (isSelectionMode) {
                   cancelBtn.visibility = View.GONE

                    if (selectedItems.contains(position)) {
                        selectBtn.visibility = View.GONE
                       completeBtn.visibility = View.VISIBLE
                    } else {
                       selectBtn.visibility = View.VISIBLE
                        completeBtn.visibility = View.GONE
                    }
                } else {
                    cancelBtn.visibility = View.VISIBLE
                   selectBtn.visibility = View.GONE
                  completeBtn.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(position)
                    } else {
                        try {

                            val currentFragment = LinkTubeActivity.tabsList[position].fragment
                            if (currentFragment is BrowseFragment && !currentFragment.binding.webView.url.isNullOrEmpty()) {
                                reloadBrowserFragment(currentFragment)
                            }
                            LinkTubeActivity.myPager.currentItem = position
                         notifyDataSetChanged()
                            if (context is TabActivity) {
                                TabQuickButtonAdapter.updateTabs()
                                context.finish()
                            }
                        } catch (e: Exception) {
                            showErrorToast()
                        }
                    }
                }

                cancelBtn.setOnClickListener {
                    try {
                        if (position == 0) {
                            Toast.makeText(context, "Cannot remove the first tab", Toast.LENGTH_LONG).show()

                        } else {
                            LinkTubeActivity.tabsList.removeAt(position)
                            notifyDataSetChanged()
                            LinkTubeActivity.myPager.adapter?.notifyItemRemoved(position)
                            TabQuickButtonAdapter.updateTabs()

                        }
                        if (LinkTubeActivity.myPager.currentItem == position) {
                            LinkTubeActivity.myPager.currentItem = position - 1
                        } else if (LinkTubeActivity.tabsList.isNotEmpty()) {
                            LinkTubeActivity.myPager.currentItem = 0
                        }
                    } catch (e: Exception) {
                        showErrorToast()
                    }
                }
            } catch (e: Exception) {
                showErrorToast()
            }

            }
        private fun toggleSelection( position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
              selectBtn.visibility = View.VISIBLE
                completeBtn.visibility = View.GONE
            } else {
                selectedItems.add(position)
               selectBtn.visibility = View.GONE
               completeBtn.visibility = View.VISIBLE
            }

            if (selectedItems.isEmpty()) {
                actionMode?.title = "Select tabs"
            } else {
                actionMode?.title = "${selectedItems.size} Selected"
            }
        }
        }

    inner class Tab2ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cancelBtn: ImageView = itemView.findViewById(R.id.cancelBtn)
        private val name: TextView = itemView.findViewById(R.id.tabName)
        private val websiteIcon: ShapeableImageView = itemView.findViewById(R.id.websiteIcon)
        private val websiteView: ShapeableImageView = itemView.findViewById(R.id.tabViewImage)

        @SuppressLint("NotifyDataSetChanged")
        fun bind(fileItem: Tab) {
            try {

                if (position == 0) {
                    // Always set the properties for the first tab
                    websiteIcon.setImageResource(R.drawable.internat_browser_white)
                    websiteView.setImageResource(R.drawable.tab_internat_browser_gray)
                    name.text = "Home"
                } else {
                    val tab = LinkTubeActivity.tabsList[position]
                    val googleSearchPrefix = "https://www.google.com/search?q="

                    // Decode and clean the tab name if it starts with the Google search prefix
                    name.text = if (tab.name.startsWith(googleSearchPrefix)) {
                        cleanTabName(Uri.decode(tab.name.removePrefix(googleSearchPrefix)))
                    } else {
                        cleanTabName(tab.name)
                    }

                    // Set the website icon if available
                    if (tab.icon != null) {
                        websiteIcon.setImageBitmap(tab.icon)
                    } else {
                        // Optionally, set a placeholder or default icon
                        websiteIcon.setImageResource(R.drawable.internat_browser_white)
                    }

                    // Set the preview bitmap to websiteView
                    if (tab.previewBitmap != null) {
                        websiteView.setImageBitmap(tab.previewBitmap)
                    } else {
                        // Set a placeholder or default icon if the previewBitmap is not set
                        websiteView.setImageResource(R.drawable.tab_internat_browser_gray)
                    }
                }

                itemView.setOnClickListener {
                        try {
                            val currentFragment = LinkTubeActivity.tabsList[position].fragment
                            if (currentFragment is BrowseFragment && !currentFragment.binding.webView.url.isNullOrEmpty()) {
                                reloadBrowserFragment(currentFragment)
                            }
                            LinkTubeActivity.myPager.currentItem = position
                        } catch (e: Exception) {
                            showErrorToast()
                        }

                }

                cancelBtn.setOnClickListener {
                    try {
                        if (position == 0) {
                            Toast.makeText(context, "Cannot remove the first tab", Toast.LENGTH_LONG).show()

                        } else {
                            LinkTubeActivity.tabsList.removeAt(position)
                            notifyDataSetChanged()
                            LinkTubeActivity.myPager.adapter?.notifyItemRemoved(position)
                            TabQuickButtonAdapter.updateTabs()

                        }
                        if (LinkTubeActivity.myPager.currentItem == position) {
                            LinkTubeActivity.myPager.currentItem = position - 1
                        } else if (LinkTubeActivity.tabsList.isNotEmpty()) {
                            LinkTubeActivity.myPager.currentItem = 0
                        }
                    } catch (e: Exception) {
                        showErrorToast()
                    }
                }
            } catch (e: Exception) {
                showErrorToast()
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isLinktubeActivity) {
            val apkView = inflater.inflate(R.layout.tab_layout_dialog, parent, false)
            Tab2ViewHolder(apkView)
        }
        else {
            val MHTMLView = inflater.inflate(R.layout.tab_layout, parent, false)
            TabViewHolder(MHTMLView)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fileItem = LinkTubeActivity.tabsList[position]
        if (isLinktubeActivity) {
            (holder as Tab2ViewHolder).bind(fileItem)

        }
        else {
            (holder as TabViewHolder).bind(fileItem)

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

    private fun showErrorToast() {
        Toast.makeText(context, "Error occurred, restart the app", Toast.LENGTH_LONG).show()
    }



    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelectionMode(actionMode: ActionMode) {
        isSelectionMode = !isSelectionMode
        this.actionMode = actionMode
        actionMode.title  = "Select tabs" // Set default title
        selectedItems.clear()
        TabQuickButtonAdapter.updateTabs()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeSelectedTabs() {
        val selectedPositions = selectedItems.sortedDescending()
        for (position in selectedPositions) {
            if (position != 0) {
                LinkTubeActivity.tabsList.removeAt(position)
                LinkTubeActivity.myPager.adapter?.notifyItemRemoved(position)
                if (LinkTubeActivity.myPager.currentItem == position) {
                    LinkTubeActivity.myPager.currentItem = position - 1
                } else if (LinkTubeActivity.tabsList.isNotEmpty()) {
                    LinkTubeActivity.myPager.currentItem = 0
                }
            }

        }
        selectedItems.clear()
        actionMode?.finish()

        notifyDataSetChanged()

    }
}
