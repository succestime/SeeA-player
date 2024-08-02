package com.jaidev.seeaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.MainActivity.Companion.videoList
import com.jaidev.seeaplayer.allAdapters.VideoSearchAdapter
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.ActivitySearchBinding
import java.io.File

class SearchActivity : AppCompatActivity()  , VideoSearchAdapter.OnItemClickListener{

    private lateinit var videoSearchAdapter: VideoSearchAdapter
    private lateinit var SearchAdapter: VideoSearchAdapter
    private lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem
    private lateinit var swipeRefreshLayout: ConstraintLayout

    var isSearchViewClicked = false
companion object{
    private lateinit var binding: ActivitySearchBinding

}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val videoData: VideoData? = intent.getParcelableExtra("videoData")
        setActionBarGradient()
        if (videoData != null) {
            val videoList = ArrayList<VideoData>()
            videoList.add(videoData)

            videoSearchAdapter = VideoSearchAdapter(this, videoList , isSearchActivity = true, isFolder = false, isShort = false , this
            )
            binding.videoSearchRV.layoutManager = LinearLayoutManager(this)
            binding.videoSearchRV.adapter = videoSearchAdapter
            // Set the action bar title
            supportActionBar?.title = "Search for '${videoData.title}'"
        }

        SearchAdapter = VideoSearchAdapter(this@SearchActivity, videoList, isSearchActivity = false , isFolder = true,
            isShort = true , this)
       binding.recyclerView.setHasFixedSize(true)
       binding.recyclerView.setItemViewCacheSize(10)
       binding.recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity)
       binding.recyclerView.visibility = View.GONE
       binding.recyclerView.adapter = SearchAdapter

        swipeRefreshLayout = binding.swipeRefreshLayout

        setSwipeRefreshBackgroundColor()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_view_menu, menu)
        searchItem = menu.findItem(R.id.searchView)

        searchView = searchItem.actionView as SearchView

        searchView.setOnCloseListener {
            isSearchViewClicked = false
            binding.recyclerView.visibility = View.GONE
            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                MainActivity.searchList = ArrayList()
                if (newText != null) {
                    val queryText = newText.lowercase()
                    for (video in videoList) {
                        if (video.title.lowercase().contains(queryText)) {
                            MainActivity.searchList.add(video)
                        }
                    }
                    MainActivity.search = true
                    SearchAdapter.updateList(searchList = MainActivity.searchList)
                }
                if (isSearchViewClicked || newText?.isNotEmpty() == true) {
                    binding.recyclerView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.GONE
                }
                return true
            }
        })

        return true
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onItemClick(fileItem: VideoData) {
        val videoUri = Uri.fromFile(File(fileItem.path)).toString()
        openPlayerActivity(videoUri, fileItem.title)
    }





    private fun openPlayerActivity(videoUri: String, videoTitle: String) {
        val intent = Intent(this, PlayerFileActivity::class.java).apply {
            putExtra("videoUri", videoUri)
            putExtra("videoTitle", videoTitle)
        }
        startActivity(intent)
    }


    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    private fun setActionBarGradient() {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@SearchActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@SearchActivity,
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
                            this@SearchActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@SearchActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }

}
