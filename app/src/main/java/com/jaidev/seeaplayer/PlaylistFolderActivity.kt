package com.jaidev.seeaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.allAdapters.PlaylistAdapter
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.OnPlaylistCreatedListener
import com.jaidev.seeaplayer.dataClass.PlaylistEntity
import com.jaidev.seeaplayer.dataClass.PlaylistVideo
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityPlaylistFolderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistFolderActivity : AppCompatActivity(), OnPlaylistCreatedListener {
    private lateinit var binding: ActivityPlaylistFolderBinding
    private lateinit var playlistAdapter: PlaylistAdapter // Adapter for the RecyclerView
    private val db by lazy { DatabaseClient.getInstance(this) }

    private val updatePlaylistReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Refresh the playlists
            refreshPlaylists()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        binding = ActivityPlaylistFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            updatePlaylistReceiver,
            IntentFilter("UPDATE_PLAYLIST_FOLDER")
        )

        // Load playlists
        loadPlaylists()

        // Set up menu item click listener
        binding.playlistToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.closePlaylist -> {
                    // Handle the first menu item click
                    finish()
                    true
                }

                else -> false
            }
        }// Initialize RecyclerView
        playlistAdapter = PlaylistAdapter(this , mutableListOf())
        binding.playlistRecyclerview.apply {
            layoutManager = LinearLayoutManager(this@PlaylistFolderActivity)
            adapter = playlistAdapter
        }
        updateNoPlaylistsTextVisibility()

        binding.createPlaylistButton.setOnClickListener {
            val bottomSheet = CreatePlaylistBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        setSwipeRefreshBackgroundColor()
        loadPlaylistsFromDatabase()
    }

    override fun onPlaylistCreated(playlistName: String) {
        lifecycleScope.launch {
            val newPlaylistId = insertPlaylist(playlistName)
            withContext(Dispatchers.Main) {
                val newPlaylist = PlaylistVideo(newPlaylistId, playlistName)
                playlistAdapter.addPlaylist(newPlaylist)
                updateNoPlaylistsTextVisibility()
            }
        }
    }

    private suspend fun insertPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
        }
    }


    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    private fun updateNoPlaylistsTextVisibility() {
        if (playlistAdapter.isEmpty()) {
            binding.noPlaylistsText.visibility = View.VISIBLE
        } else {
            binding.noPlaylistsText.visibility = View.GONE
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatePlaylistReceiver)

    }

    private fun refreshPlaylists() {
        // Reload the playlists from the database
        loadPlaylists()
        loadPlaylistsFromDatabase()

    }
    private fun loadPlaylists() {
        lifecycleScope.launch {
            // Fetch playlists from the database
            val playlists = withContext(Dispatchers.IO) {
                db.playlistDao().getAllPlaylists()
            }
            // Update the adapter with the fetched playlists
            playlistAdapter.updatePlaylists(playlists.map { PlaylistVideo(it.id, it.name) })
            // Update the visibility of the "No Playlists" text
            updateNoPlaylistsTextVisibility()
        }
    }

    private fun loadPlaylistsFromDatabase() {
        lifecycleScope.launch {
            val playlists = withContext(Dispatchers.IO) {
                db.playlistDao().getAllPlaylists()
            }
            playlistAdapter.updatePlaylists(playlists.map { PlaylistVideo(it.id, it.name) })
            updateNoPlaylistsTextVisibility()
        }
    }
}