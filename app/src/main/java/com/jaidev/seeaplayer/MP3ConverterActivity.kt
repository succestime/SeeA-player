
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.allAdapters.MP3Adapter
import com.jaidev.seeaplayer.dataClass.MP3AppDatabase
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.dataClass.MP3FileEntity
import com.jaidev.seeaplayer.dataClass.MP3FileObserver
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityMp3ConverterBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MP3ConverterActivity : AppCompatActivity() {

    private lateinit var mp3Adapter: MP3Adapter
    private lateinit var binding: ActivityMp3ConverterBinding
    private val db by lazy { MP3AppDatabase.getDatabase(this) }
    private lateinit var noPlaylistsText: TextView
    private lateinit var mp3FileObserver: MP3FileObserver

    companion object {
        const val ACTION_HIDE_NOW_PLAYING = "com.jaidev.seeaplayer.ACTION_HIDE_NOW_PLAYING"
    }

    private val hideNowPlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadMP3Files()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        binding = ActivityMp3ConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        registerReceiver(hideNowPlayingReceiver, IntentFilter(ACTION_HIDE_NOW_PLAYING))
        setSwipeRefreshBackgroundColor()
        noPlaylistsText = findViewById(R.id.no_playlists_text)
        val convertedMP3RecyclerView = findViewById<RecyclerView>(R.id.convertedMP3RecyclerView)
        mp3Adapter = MP3Adapter(this, MainActivity.MP3MusicList)
        convertedMP3RecyclerView.adapter = mp3Adapter
        convertedMP3RecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize MP3FileObserver with a callback to update the UI on file deletion
        mp3FileObserver = MP3FileObserver(this) { deletedFilePath ->
            onMP3FileDeleted(deletedFilePath)
        }
        mp3FileObserver.startWatching()
        loadMP3Files()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun loadMP3Files() {
        binding.progressBar.visibility = View.VISIBLE
        mp3FileObserver.startWatching()
        GlobalScope.launch(Dispatchers.IO) {
            val mp3FilesFromDb = db.mp3FileDao().getAllMP3Files()

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE

                MainActivity.MP3MusicList.clear()
                MainActivity.MP3MusicList.addAll(mp3FilesFromDb.map { entity ->
                    MP3FileData(
                        id = entity.id,
                        title = entity.title,
                        duration = entity.duration,
                        size = entity.size,
                        dateAdded = entity.dateAdded,
                        path = entity.path,
                        artUri = entity.artUri
                    )
                })
                if (MainActivity.MP3MusicList.isEmpty()) {
                    noPlaylistsText.visibility = View.VISIBLE
                } else {
                    noPlaylistsText.visibility = View.GONE
                }
                mp3Adapter.notifyDataSetChanged()
            }
        }
    }

    // Handle deletion updates
    private fun onMP3FileDeleted(deletedFilePath: String) {
        val index = MainActivity.MP3MusicList.indexOfFirst { it.path == deletedFilePath }
        if (index != -1) {
            MainActivity.MP3MusicList.removeAt(index)
            mp3Adapter.notifyItemRemoved(index)
        }
        if (MainActivity.MP3MusicList.isEmpty()) {
            noPlaylistsText.visibility = View.VISIBLE
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

    @OptIn(DelicateCoroutinesApi::class)
    fun addMP3File(mp3FileData: MP3FileData) {
        MainActivity.MP3MusicList.add(mp3FileData)
        mp3Adapter.notifyItemInserted(MainActivity.MP3MusicList.size - 1)
        mp3FileObserver.startWatching()
        GlobalScope.launch(Dispatchers.IO) {
            db.mp3FileDao().insert(
                MP3FileEntity(
                    id = mp3FileData.id,
                    title = mp3FileData.title,
                    duration = mp3FileData.duration,
                    size = mp3FileData.size,
                    dateAdded = mp3FileData.dateAdded,
                    path = mp3FileData.path,
                    artUri = mp3FileData.artUri
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mp3FileObserver.stopWatching()
        unregisterReceiver(hideNowPlayingReceiver)
    }
}

