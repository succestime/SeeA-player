package com.jaidev.seeaplayer.browserActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter
import com.jaidev.seeaplayer.databinding.ActivityBookmarkBinding

class BookmarkActivity : AppCompatActivity() {
    private lateinit var allBookMarkLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "All Bookmarks"
        binding.rvBookmarks.setItemViewCacheSize(5)
        binding.rvBookmarks.hasFixedSize()
        binding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        binding.rvBookmarks.adapter = BookmarkAdapter(this , isActivity = true)

        supportActionBar?.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@BookmarkActivity,
                    R.drawable.background_actionbar
                )
            )
        }

        setActionBarGradient()
        allBookMarkLayout = binding.allBookmarkLayout

        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()


    }

    private fun setRelativeLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            allBookMarkLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            allBookMarkLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
          supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                       this@BookmarkActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }

        }
    }
}