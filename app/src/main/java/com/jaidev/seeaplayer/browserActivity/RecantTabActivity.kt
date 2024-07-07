package com.jaidev.seeaplayer.browserActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.HistoryAdapter
import com.jaidev.seeaplayer.allAdapters.RecentTabAdapter
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.databinding.ActivityRecantTabActivityBinding

class RecantTabActivity : AppCompatActivity(), RecentTabAdapter.ItemClickListener{
    private lateinit var binding: ActivityRecantTabActivityBinding
    private lateinit var recantTabActivity: ConstraintLayout
    private lateinit var recentTabAdapter: RecentTabAdapter
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyList: MutableList<HistoryItem>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecantTabActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
         supportActionBar?.title = "Recent tabs"


        historyList = HistoryManager.getHistoryList(this).toMutableList()
        val recentItems = historyList.take(6).toMutableList() // Fetch 6 items initially
        recentTabAdapter = RecentTabAdapter(this, recentItems, this)
        binding.RecantTabRV.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RecantTabActivity)
            adapter = recentTabAdapter
        }

        binding.showFullHistory.setOnClickListener {
            startActivity(Intent(this, HistoryBrowser::class.java))
        }
        binding.recantCosed.setOnClickListener {
            toggleVisibility()
        }
        setActionBarGradient()
        recantTabActivity = binding.recantTabActivity
        setSwipeRefreshBackgroundColor()
    }




    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setActionBarGradient() {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@RecantTabActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@RecantTabActivity,
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
                            this@RecantTabActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@RecantTabActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            recantTabActivity.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            recantTabActivity.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    private fun toggleVisibility() {
        if (binding.RecantTabRV.visibility == View.VISIBLE) {
            binding.RecantTabRV.visibility = View.GONE
            binding.showFullHistory.visibility = View.GONE
            binding.iconArrow.setImageResource(R.drawable.round_arrow_down_24)
        } else {
            binding.RecantTabRV.visibility = View.VISIBLE
            binding.showFullHistory.visibility = View.VISIBLE
            binding.iconArrow.setImageResource(R.drawable.round_arrow_up_24)
        }
    }



    override fun onItemClick(historyItem: HistoryItem) {
        openUrlInBrowser(historyItem.url)
        finish()

    }




    private fun openUrlInBrowser(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }

}