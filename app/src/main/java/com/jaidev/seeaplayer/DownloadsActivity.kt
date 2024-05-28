package com.jaidev.seeaplayer

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.jaidev.seeaplayer.databinding.ActivityDownloadsBinding

class DownloadsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDownloadsBinding
    private lateinit var downloadsActivity: ConstraintLayout
    private lateinit var downloadedActivity: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActionBarGradient()
        downloadsActivity = binding.DownloadsActivity
        downloadedActivity = binding.downloadedConstraintLayout

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }



    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            downloadsActivity.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            downloadedActivity.setBackgroundColor(resources.getColor(R.color.black_statusBar))
        } else {
            // Light mode is enabled, set background color to white
            downloadsActivity.setBackgroundColor(resources.getColor(android.R.color.white))
            downloadedActivity.setBackgroundColor(resources.getColor(R.color.light_statusBar))
        }
    }
    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@DownloadsActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@DownloadsActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            // System Default mode is applied
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            // Set the ActionBar color based on the System Default mode
            if (isSystemDefaultDarkMode) {
                // System Default mode is dark
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@DownloadsActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@DownloadsActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

}