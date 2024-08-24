package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.allAdapters.MP3Adapter
import com.jaidev.seeaplayer.dataClass.MP3AppDatabase
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.dataClass.MP3FileEntity
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MP3ConverterActivity : AppCompatActivity() {

    private lateinit var mp3Adapter: MP3Adapter
    private val mp3Files = ArrayList<MP3FileData>()
    private lateinit var nowPlayingFragmentContainer: FragmentContainerView
    private var mp3FileTitle: String? = null

    private val nowPlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            val trackTitle = intent.getStringExtra("trackTitle")

            if (isPlaying) {
                nowPlayingFragmentContainer.visibility = View.VISIBLE

                // Optionally, update the "Now Playing" fragment with the track title
            } else {
                nowPlayingFragmentContainer.visibility = View.GONE

            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        setContentView(R.layout.activity_mp3_converter)
supportActionBar?.hide()

        nowPlayingFragmentContainer = findViewById(R.id.nowPlaying)

        // Register the receiver to listen for now playing broadcasts
        registerReceiver(nowPlayingReceiver, IntentFilter("NOW_PLAYING_ACTION"),
            RECEIVER_NOT_EXPORTED
        )

        val convertedMP3RecyclerView = findViewById<RecyclerView>(R.id.convertedMP3RecyclerView)
        mp3Adapter = MP3Adapter(this ,mp3Files)
        convertedMP3RecyclerView.adapter = mp3Adapter
        convertedMP3RecyclerView.layoutManager = LinearLayoutManager(this)


        loadMP3Files()
    }


    // Method to get the service

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun loadMP3Files() {
        GlobalScope.launch(Dispatchers.IO) {
            val db = MP3AppDatabase.getDatabase(this@MP3ConverterActivity)
            val mp3FilesFromDb = db.mp3FileDao().getAllMP3Files()

            withContext(Dispatchers.Main) {
                mp3Files.clear()
                mp3Files.addAll(mp3FilesFromDb.map { entity ->
                    MP3FileData(
                        id = entity.id,
                        title = entity.title,
                        duration = entity.duration,
                        size = entity.size,
                        dateAdded = entity.dateAdded,
                        path = entity.path
                    )
                })
                mp3Adapter.notifyDataSetChanged()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun addMP3File(mp3FileData: MP3FileData) {
        mp3Files.add(mp3FileData)
        mp3Adapter.notifyItemInserted(mp3Files.size - 1)

        GlobalScope.launch(Dispatchers.IO) {
            val db = MP3AppDatabase.getDatabase(this@MP3ConverterActivity)
            db.mp3FileDao().insert(
                MP3FileEntity(
                id = mp3FileData.id,
                title = mp3FileData.title,
                duration = mp3FileData.duration,
                size = mp3FileData.size,
                dateAdded = mp3FileData.dateAdded,
                path = mp3FileData.path
            )
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(nowPlayingReceiver)
    }
}
