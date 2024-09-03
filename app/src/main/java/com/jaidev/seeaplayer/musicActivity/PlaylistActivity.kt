
package com.jaidev.seeaplayer.musicActivity

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
import com.jaidev.seeaplayer.CreatePlaylistMusicBottomSheetFragment
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.PlaylistViewAdapter
import com.jaidev.seeaplayer.dataClass.DatabaseClientMusic
import com.jaidev.seeaplayer.dataClass.OnPlaylistMusicCreatedListener
import com.jaidev.seeaplayer.dataClass.PlaylistMusic
import com.jaidev.seeaplayer.dataClass.PlaylistMusicEntity
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityPlaylistBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlaylistActivity : AppCompatActivity() , OnPlaylistMusicCreatedListener {
    lateinit var binding: ActivityPlaylistBinding
    private lateinit var playlistAdapter : PlaylistViewAdapter
    private val db by lazy { DatabaseClientMusic.getInstance(this) }

    private val updatePlaylistReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshPlaylists()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            updatePlaylistReceiver,
            IntentFilter("UPDATE_PLAYLIST_MUSIC")
        )

// Refresh the playlists
        refreshPlaylists()
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
        playlistAdapter = PlaylistViewAdapter(this , mutableListOf())
        binding.playlistRV.apply {
            layoutManager = LinearLayoutManager(this@PlaylistActivity)
            adapter = playlistAdapter
        }
        updateNoPlaylistsTextVisibility()

        binding.addPlaylistBtn.setOnClickListener {
            val bottomSheet = CreatePlaylistMusicBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        setSwipeRefreshBackgroundColor()

    }
    private suspend fun removeDeletedSongsFromPlaylist(deletedMusicPath: String) {
        withContext(Dispatchers.IO) {
            val deletedMusic = db.playlistMusicDao().getMusicByPath(deletedMusicPath)
            if (deletedMusic != null) {
                // Remove the music from the playlist
                db.playlistMusicDao().deleteMusicFromPlaylist(PlaylistDetails.playlistId, deletedMusic.musicid)

                // Remove the music from the database
                db.playlistMusicDao().deleteMusic(deletedMusic.musicid)
            }
        }
        withContext(Dispatchers.Main) {
            refreshPlaylists()

        }
    }
    private suspend fun checkForDeletedSongs() {
        withContext(Dispatchers.IO) {
            val allMusic = db.playlistMusicDao().getAllMusic()
            for (music in allMusic) {
                if (!File(music.path).exists()) {
                    removeDeletedSongsFromPlaylist(music.path)
                }
            }
        }
    }
    override fun onPlaylistMusicCreated(playlistName: String) {
        lifecycleScope.launch {
            val newPlaylistId = insertPlaylist(playlistName)
            withContext(Dispatchers.Main) {
                val newPlaylist = PlaylistMusic(newPlaylistId, playlistName)
                playlistAdapter.addPlaylist(newPlaylist)
                updateNoPlaylistsTextVisibility()
            }
        }
    }

    private suspend fun insertPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            db.playlistMusicDao().insertPlaylist(PlaylistMusicEntity(name = name))
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
            checkForDeletedSongs()
            val playlists = withContext(Dispatchers.IO) {
                db.playlistMusicDao().getAllPlaylists()
            }
            // Update the adapter with the fetched playlists
            playlistAdapter.updatePlaylists(playlists.map { PlaylistMusic(it.musicid, it.name) })
            // Update the visibility of the "No Playlists" text
            updateNoPlaylistsTextVisibility()
        }
    }

    private fun loadPlaylistsFromDatabase() {
        lifecycleScope.launch {
            checkForDeletedSongs()
            val playlists = withContext(Dispatchers.IO) {
                db.playlistMusicDao().getAllPlaylists()
            }
            playlistAdapter.updatePlaylists(playlists.map { PlaylistMusic(it.musicid, it.name) })
            updateNoPlaylistsTextVisibility()
        }
    }




}