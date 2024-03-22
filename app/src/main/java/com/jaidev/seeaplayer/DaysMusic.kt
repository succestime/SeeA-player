package com.jaidev.seeaplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jaidev.seeaplayer.MainActivity.Companion.musicRecantList
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.databinding.FragmentDaysMusicBinding
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import java.util.concurrent.TimeUnit

class DaysMusic : Fragment() {
    private lateinit var binding: FragmentDaysMusicBinding
    private lateinit var adapter: RecantMusicAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
   val view =  inflater.inflate(R.layout.fragment_days_music, container, false)
        binding = FragmentDaysMusicBinding.bind(view)
       binding.MusicRV.setHasFixedSize(true)
        binding.MusicRV.setItemViewCacheSize(13)
        binding.MusicRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecantMusicAdapter(requireContext(), musicRecantList , isReMusic = true)
        binding.MusicRV.adapter = adapter
        binding.daysTotalMusics.text = "Total Musics : 0"
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadRecentMusics()
            } else {

                // Handle permission denial
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadRecentMusics()
        } else {

            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            binding.swipeRefreshMusic.isRefreshing = false // Hide the refresh indicator
        }, 2000) // 2000 milliseconds (2 seconds)


        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(requireContext() , ReMusicPlayerActivity::class.java)
            intent.putExtra("index" , 0)
            intent.putExtra("class" , "DaysMusic")
            startActivity(intent)
        }
        if (musicRecantList.isEmpty()) {
            binding.shuffleBtn.visibility = View.GONE
        } else {
            binding.shuffleBtn.visibility = View.VISIBLE
        }

        swipeRefreshLayout = binding.swipeRefreshMusic

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()


        return view
    }
    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentMusics() {
        val recantMusics = getAllRecantMusics(requireContext())
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val reMusics = recantMusics.filter { it.timestamp >= sevenDaysAgo }
        recantMusics.sortedByDescending { it.timestamp }
        adapter.updateRecentMusics(reMusics)

        // Update the total music count text
        binding.daysTotalMusics.text = "Total Musics : ${reMusics.size}"

        if (musicRecantList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }

    }

    private fun getAllRecantMusics(context: Context): ArrayList<RecantMusic> {
        val musicReList = ArrayList<RecantMusic>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID ,
            MediaStore.Audio.Media.SIZE
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val titleC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val sizeC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdC =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) // Add ALBUM_ID index
            val idC = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val durationC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val timestampC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathC = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val title = it.getString(titleC)
                val artist = it.getString(artistC)
                val album = it.getString(albumC)
                val size = it.getString(sizeC)
                val albumId = it.getLong(albumIdC) // Get ALBUM_ID
                val id = it.getString(idC)
                val duration = it.getLong(durationC)
                val timestamp = it.getLong(timestampC) * 1000 // Convert to milliseconds
                val path = it.getString(pathC)

                // Construct album art URI
                val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

                val music = RecantMusic(title, artist, album, timestamp, id, duration, path, albumArtUri , size)
                musicReList.add(music)
            }
        }

        return musicReList
    }


}