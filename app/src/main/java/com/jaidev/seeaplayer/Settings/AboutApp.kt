package com.jaidev.seeaplayer.Settings

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.ActivityAboutAppBinding

class AboutApp : AppCompatActivity() {
    private lateinit var binding: ActivityAboutAppBinding
    private lateinit var swipeRefreshLayout: ScrollView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "About SeeA Player"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setActionBarGradient()
        swipeRefreshLayout = binding.aboutAppActivity
        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()

        // Find TextViews by their IDs
        val textView1: TextView = findViewById(R.id.about_para_1)
        val textView2: TextView = findViewById(R.id.about_para_2)
        val textView3: TextView = findViewById(R.id.about_para_3)
        val textView4: TextView = findViewById(R.id.about_para_4)
        val textView5: TextView = findViewById(R.id.about_para_5)
        val textView6: TextView = findViewById(R.id.about_para_6)


// Set text style to bold for specific words
        var aboutPara1 = resources.getString(R.string.about_para_1)
        aboutPara1 = aboutPara1.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara1 = aboutPara1.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView1.text = HtmlCompat.fromHtml(aboutPara1, HtmlCompat.FROM_HTML_MODE_LEGACY)

        var aboutPara2 = resources.getString(R.string.about_para_2)
        aboutPara2 = aboutPara2.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara2 = aboutPara2.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView2.text = HtmlCompat.fromHtml(aboutPara2, HtmlCompat.FROM_HTML_MODE_LEGACY)

        var aboutPara3 = resources.getString(R.string.about_para_3)
        aboutPara3 = aboutPara3.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara3 = aboutPara3.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView3.text = HtmlCompat.fromHtml(aboutPara3, HtmlCompat.FROM_HTML_MODE_LEGACY)

        var aboutPara4 = resources.getString(R.string.about_para_4)
        aboutPara4 = aboutPara4.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara4 = aboutPara4.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView4.text = HtmlCompat.fromHtml(aboutPara4, HtmlCompat.FROM_HTML_MODE_LEGACY)

        var aboutPara5 = resources.getString(R.string.about_para_5)
        aboutPara5 = aboutPara5.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara5 = aboutPara5.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView5.text = HtmlCompat.fromHtml(aboutPara5, HtmlCompat.FROM_HTML_MODE_LEGACY)

        var aboutPara6 = resources.getString(R.string.about_para_6)
        aboutPara6 = aboutPara6.replace("SeeA Player", "<b>SeeA Player</b>")
        aboutPara6 = aboutPara6.replace("PowerOI Group", "<b>PowerOI Group</b>")
        textView6.text = HtmlCompat.fromHtml(aboutPara6, HtmlCompat.FROM_HTML_MODE_LEGACY)

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

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
                        this@AboutApp,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@AboutApp,
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
                            this@AboutApp,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@AboutApp,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }
}