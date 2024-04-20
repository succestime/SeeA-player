package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.databinding.ActivityPlaylistItemsBinding

class PlaylistItems : AppCompatActivity() {
    private lateinit var playlistdetailsLayout: ConstraintLayout
    private lateinit var binding : ActivityPlaylistItemsBinding
    private lateinit var adapter: MusicAdapter // Define your adapter property here

    companion object {
        var currentPlaylistPos: Int = -1
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Playlist Album"
        // Initialize adapter
        adapter = MusicAdapter(this, ArrayList())

        binding.playlistDetailsRV.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(13)
            layoutManager = LinearLayoutManager(this@PlaylistItems)
            adapter = this@PlaylistItems.adapter // Set adapter to RecyclerView
        }

        binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerMusicActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }

        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
            finish()
        }


        supportActionBar?.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@PlaylistItems,
                    R.drawable.background_actionbar
                )
            )
        }
        setActionBarGradient()
        playlistdetailsLayout = binding.playlistDetailsLayout

        // Set the background color of ConstraintLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            playlistdetailsLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            playlistdetailsLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@PlaylistItems,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        }
    }
}
