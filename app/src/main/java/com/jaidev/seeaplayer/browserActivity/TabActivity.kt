package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.TabAdapter
import com.jaidev.seeaplayer.browseFregment.HomeFragment
import com.jaidev.seeaplayer.databinding.ActivityTabBinding

class TabActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTabBinding
    private lateinit var tabLayout: ConstraintLayout
    private lateinit var adapter: TabAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityTabBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.tabsRV.setHasFixedSize(true)
            binding.tabsRV.layoutManager = GridLayoutManager(this, 2)
            adapter = TabAdapter(this)
            binding.tabsRV.adapter = adapter



            setActionBarGradient()
            updateEmptyViewVisibility()
            setupActionBar()

            tabLayout = binding.tabActivityConstraintLayout
            setSwipeRefreshBackgroundColor()


        } catch (e: Exception) {
            showErrorToast()
        }
    }

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

            // Check if the RecyclerView is empty
            if ((binding.tabsRV.adapter as TabAdapter).itemCount == 0) {
                // Disable and set the color of the menu items
                val clearAllTabsItem = popup.menu.findItem(R.id.clearAllTabs)
                val selectTabsItem = popup.menu.findItem(R.id.selectTabs)

                clearAllTabsItem.isEnabled = false
                selectTabsItem.isEnabled = false

                val grayColor = ContextCompat.getColor(this, R.color.gray) // Replace R.color.gray with your actual gray color resource

                val clearAllTabsTitle = SpannableString(clearAllTabsItem.title)
                clearAllTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, clearAllTabsTitle.length, 0)
                clearAllTabsItem.title = clearAllTabsTitle

                val selectTabsTitle = SpannableString(selectTabsItem.title)
                selectTabsTitle.setSpan(ForegroundColorSpan(grayColor), 0, selectTabsTitle.length, 0)
                selectTabsItem.title = selectTabsTitle

                // If you have icons, you can set their color as well
                if (clearAllTabsItem.icon != null) {
                    clearAllTabsItem.icon!!.mutate().setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
                }
                if (selectTabsItem.icon != null) {
                    selectTabsItem.icon!!.mutate().setColorFilter(grayColor, PorterDuff.Mode.SRC_IN)
                }
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
                        changeTab("Home", HomeFragment())
                        finish()
                        true
                    }
                    R.id.clearAllTabs -> {
                        showClearAllTabsDialog()
                        true
                    }
                    R.id.selectTabs -> {
                        // Handle select tabs action
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

    private fun setActionBarGradient() {
        try {
            val nightMode = AppCompatDelegate.getDefaultNightMode()
            if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@TabActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@TabActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                    android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                    else -> false
                }
                if (isSystemDefaultDarkMode) {
                    supportActionBar?.apply {
                        setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                this@TabActivity,
                                R.drawable.background_actionbar
                            )
                        )
                    }
                } else {
                    supportActionBar?.apply {
                        setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                this@TabActivity,
                                R.drawable.background_actionbar_light
                            )
                        )
                    }
                }
            }
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
                changeTab("Home", HomeFragment())
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
