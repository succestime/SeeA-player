package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.TabAdapter
import com.jaidev.seeaplayer.allAdapters.TabQuickButtonAdapter
import com.jaidev.seeaplayer.browseFregment.HomeFragment
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityTabBinding

class TabActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTabBinding
    private lateinit var tabLayout: ConstraintLayout
    private lateinit var adapter: TabAdapter
    private var actionMode: ActionMode? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        try {
            binding = ActivityTabBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.tabsRV.setHasFixedSize(true)
            binding.tabsRV.layoutManager = GridLayoutManager(this, 2)
            adapter = TabAdapter(this , null, isLinktubeActivity = false)
            binding.tabsRV.adapter = adapter

            updateEmptyViewVisibility()
            setupActionBar()

            tabLayout = binding.tabActivityConstraintLayout
            setSwipeRefreshBackgroundColor()


        } catch (e: Exception) {
            showErrorToast()
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun setSwipeRefreshBackgroundColor() {
        try {
            val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }

            if (isDarkMode) {
                tabLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
                window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
            } else {
                tabLayout.setBackgroundColor(resources.getColor(android.R.color.white))
                window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            val navigationBarDividerColor = ContextCompat.getColor(this, R.color.gray)

            // This sets the navigation bar divider color. API 28+ required.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = navigationBarDividerColor
            }
        } catch (e: Exception) {
            showErrorToast()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return try {
            menuInflater.inflate(R.menu.tab_more, menu)
            true
        } catch (e: Exception) {
            showErrorToast()
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.open_more -> {
                    showPopupMenu(findViewById(R.id.open_more))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            showErrorToast()
            false
        }
    }

    @SuppressLint("NotifyDataSetChanged", "DiscouragedPrivateApi")
    private fun showPopupMenu(view: View) {
        try {
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.tab_click_more_menu, popup.menu)

            val clearAllTabsItem = popup.menu.findItem(R.id.clearAllTabs)
            val selectTabsItem = popup.menu.findItem(R.id.selectTabs)

            if (binding.videoEmptyStateLayout.visibility == View.VISIBLE) {
                // If RecyclerView is empty, disable items and set color to gray
                clearAllTabsItem.isEnabled = false
                selectTabsItem.isEnabled = false

                val grayColor = ContextCompat.getColor(this, R.color.gray)

                val clearAllTabsTitle = SpannableString(clearAllTabsItem.title)
                clearAllTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, clearAllTabsTitle.length, 0)
                clearAllTabsItem.title = clearAllTabsTitle

                val selectTabsTitle = SpannableString(selectTabsItem.title)
                selectTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, selectTabsTitle.length, 0)
                selectTabsItem.title = selectTabsTitle

                clearAllTabsItem.icon?.mutate()?.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
                selectTabsItem.icon?.mutate()?.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
            } else {
                // If RecyclerView is not empty, enable items and set color to default
                clearAllTabsItem.isEnabled = true
                selectTabsItem.isEnabled = true


                val clearAllTabsTitle = SpannableString(clearAllTabsItem.title)
                clearAllTabsItem.title = clearAllTabsTitle

                val selectTabsTitle = SpannableString(selectTabsItem.title)
                selectTabsItem.title = selectTabsTitle

            }
            // Use reflection to show icons in the popup menu
            try {
                val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldPopup.isAccessible = true
                val mPopup = fieldPopup.get(popup)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mPopup, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { menuItem ->
                if (!menuItem.isEnabled) {
                    // If the item is disabled, do nothing
                    return@setOnMenuItemClickListener true
                }
                when (menuItem.itemId) {
                    R.id.newTab -> {
                        LinkTubeActivity.myPager.currentItem = 0
                        TabQuickButtonAdapter.updateTabs()
                        finish()
                        true
                    }
                    R.id.clearAllTabs -> {
                        showClearAllTabsDialog()
                        true
                    }
                    R.id.selectTabs -> {
                        actionMode = startActionMode(actionModeCallback)
                        actionMode?.let {
                            adapter.toggleSelectionMode(it)
                        }

                        true
                    }
                    else -> false
                }
            }
            popup.show()
        } catch (e: Exception) {
            showErrorToast()
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_mode_menu -> {
                    val actionModeView = findViewById<View>(R.id.action_mode_menu)
                    showMorePopupMenu(actionModeView)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            Log.d("TabActivity", "ActionMode destroyed")
            actionMode?.let {
                adapter.toggleSelectionMode(it)
            }
            actionMode = null
        }
    }

    @SuppressLint("DiscouragedPrivateApi", "NotifyDataSetChanged")
    private fun showMorePopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.action_mode_more_menu, popup.menu)

        // Check if all items are selected
        val allSelected = adapter.selectedItems.size == adapter.itemCount

        if (allSelected) {
            // Hide the original "Select All" item
            val selectAllItem = popup.menu.findItem(R.id.selectAll)
            selectAllItem.isVisible = false

            // Show the existing "Deselect All" item
            val deselectAllItem = popup.menu.findItem(R.id.deSelect)
            deselectAllItem.isVisible = true
        } else {
            // Ensure "Select All" item is visible
            val selectAllItem = popup.menu.findItem(R.id.selectAll)
            selectAllItem.isVisible = true

            // Ensure "Deselect All" item is hidden
            val deselectAllItem = popup.menu.findItem(R.id.deSelect)
            deselectAllItem.isVisible = false
        }

        // Get references to close and share menu items
        val closeTabsItem = popup.menu.findItem(R.id.closeTabs)
        val shareTabsItem = popup.menu.findItem(R.id.shareTabs)

        // Check if no tabs are selected
        val noTabsSelected = adapter.selectedItems.isEmpty()

        if (noTabsSelected) {
            // Disable items and set color to gray
            closeTabsItem.isEnabled = false
            shareTabsItem.isEnabled = false

            val grayColor = ContextCompat.getColor(this, R.color.gray)

            val closeTabsTitle = SpannableString(closeTabsItem.title)
            closeTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, closeTabsTitle.length, 0)
            closeTabsItem.title = closeTabsTitle

            val shareTabsTitle = SpannableString(shareTabsItem.title)
            shareTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, shareTabsTitle.length, 0)
            shareTabsItem.title = shareTabsTitle

            closeTabsItem.icon?.mutate()?.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
            shareTabsItem.icon?.mutate()?.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
        } else {
            // Enable items and set color to default
            closeTabsItem.isEnabled = true
            shareTabsItem.isEnabled = true

            val closeTabsTitle = SpannableString(closeTabsItem.title)
            closeTabsItem.title = closeTabsTitle

            val shareTabsTitle = SpannableString(shareTabsItem.title)
            shareTabsItem.title = shareTabsTitle
        }
        // Force icons to show
        try {
            val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldPopup.isAccessible = true
            val mPopup = fieldPopup.get(popup)
            mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.selectAll -> {
                    // Select all items
                    for (i in 0 until adapter.itemCount) {
                        adapter.selectedItems.add(i)
                    }
                    adapter.notifyDataSetChanged()
                    updateActionModeTitle()
                    // Reopen the popup menu
                    showMorePopupMenu(view)

                    true
                }
                R.id.deSelect -> {
                    // Deselect all items
                    adapter.selectedItems.clear()
                    adapter.notifyDataSetChanged()
                    updateActionModeTitle()
                    // Reopen the popup menu
                    showMorePopupMenu(view)

                    true
                }
                R.id.closeTabs -> {
                    adapter.removeSelectedTabs()
                    updateActionModeTitle()
                    true
                }
                R.id.shareTabs -> {
                    shareSelectedTabs()

                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun shareSelectedTabs() {
        val selectedTabs = adapter.selectedItems.mapNotNull { position ->
            LinkTubeActivity.tabsList.getOrNull(position)?.name
        }

        if (selectedTabs.isNotEmpty()) {
            val shareText = selectedTabs.joinToString("\n")

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share tabs via"))
        } else {
            Toast.makeText(this, "No tabs selected to share", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateActionModeTitle() {
        actionMode?.title = if (adapter.selectedItems.isEmpty()) {
            "Select tabs"
        } else {
            "${adapter.selectedItems.size} / ${LinkTubeActivity.tabsList.size} Selected"
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showClearAllTabsDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_clear_all_tabs, null)
            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            dialogView.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<TextView>(R.id.confirmButton).setOnClickListener {
                LinkTubeActivity.tabsList.clear()
                (binding.tabsRV.adapter as TabAdapter).notifyDataSetChanged()
                updateEmptyViewVisibility()
                dialog.dismiss()

                // Finish the LinkTubeActivity and add a new tab with HomeFragment
                changeTab("Home", HomeFragment())
                (binding.tabsRV.adapter as TabAdapter).notifyDataSetChanged()

            }

            dialog.show()
        } catch (e: Exception) {
            showErrorToast()
        }
    }


    fun updateEmptyViewVisibility() {
        try {
            if (LinkTubeActivity.tabsList.isEmpty()) {
                binding.videoEmptyStateLayout.visibility = View.VISIBLE
                binding.tabsRV.visibility = View.GONE
            } else {
                binding.videoEmptyStateLayout.visibility = View.GONE
                binding.tabsRV.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            showErrorToast()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateEmptyViewVisibility()
        } catch (e: Exception) {
            showErrorToast()
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun setupActionBar() {
        try {
            val inflater = LayoutInflater.from(this)
            val customActionBarView = inflater.inflate(R.layout.tab_custom_action_bar_layout, null)

            val titleTextView = customActionBarView.findViewById<TextView>(R.id.titleNewTab)
            titleTextView.text = "New tab"

            val connectivityCardView = customActionBarView.findViewById<LinearLayout>(R.id.connectivityCardView)

            connectivityCardView.setOnClickListener {
                LinkTubeActivity.myPager.currentItem = 0
                TabQuickButtonAdapter.updateTabs()
                finish()
            }

            supportActionBar?.apply {
                setDisplayShowCustomEnabled(true)
                setDisplayShowTitleEnabled(false)
                customView = customActionBarView
            }
        } catch (e: Exception) {
            showErrorToast()
        }
    }

    private fun showErrorToast() {
        Toast.makeText(this, "Error occurred, restart the app", Toast.LENGTH_LONG).show()
    }
}
