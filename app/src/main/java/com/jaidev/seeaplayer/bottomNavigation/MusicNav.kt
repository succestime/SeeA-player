

package com.jaidev.seeaplayer


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.jaidev.seeaplayer.MainActivity.Companion.MusicListMA
import com.jaidev.seeaplayer.MainActivity.Companion.musicListSearch
import com.jaidev.seeaplayer.MainActivity.Companion.search
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.databinding.FragmentMusicNavBinding
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistActivity
import java.io.File


class musicNav : Fragment(), MusicAdapter.MusicDeleteListener  {

    private lateinit var binding: FragmentMusicNavBinding
    lateinit var adapter: MusicAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_music_nav, container, false)
        binding = FragmentMusicNavBinding.bind(view)
        binding.musicRV.setHasFixedSize(true)
        binding.musicRV.setItemViewCacheSize(10)
        binding.musicRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = MusicAdapter(requireContext(), MusicListMA, isMusic = true)
        adapter.setMusicDeleteListener(this)

        binding.musicRV.adapter = adapter
        binding.TotalMusics.text = "Total Musics : ${MusicListMA.size}"


        binding.swipeRefreshMusic.setOnRefreshListener {
            // Perform the refresh action here
            refreshMusic()
        }
        if (MusicListMA.isEmpty()) {
            binding.musicemptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.musicemptyStateLayout.visibility = View.GONE
        }

        binding.shuffleBtn.setOnClickListener {
            val intent = Intent(requireContext() , PlayerMusicActivity::class.java)
            intent.putExtra("index" , 0)
            intent.putExtra("class" , "MusicNav")
            startActivity(intent)
        }


        binding.favouriteBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FavouriteActivity::class.java))
        }
        binding.playlistBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PlaylistActivity
            ::class.java))
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
    private fun requestRuntimePermission(): Boolean {
        // Check for permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                    13
                )
                return false
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    13
                )
                return false
            }
        } else {
            // For Android versions >= Q (API 29)
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    14
                )
                return false
            }
        }
        return true
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            // Handle WRITE_EXTERNAL_STORAGE or READ_MEDIA_VIDEO permission request result
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load recent videos
                MusicListMA = getAllAudios()
            } else {
                // Permission denied, show a message or retry request
                Snackbar.make(binding.root, "Storage Permission Needed!!", Snackbar.LENGTH_LONG).show()
            }
        } else if (requestCode == 14) {
            // Handle READ_EXTERNAL_STORAGE permission request result
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load recent videos
                MusicListMA = getAllAudios()
            } else {
                // Permission denied, show a message or retry request
                Snackbar.make(binding.root, "Storage Permission Needed!!", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    @SuppressLint("Range", "SuspiciousIndentation")
    fun getAllAudios(): ArrayList<Music> {
        val sortMusicEditor = requireContext().getSharedPreferences("Sorting", AppCompatActivity.MODE_PRIVATE)
        MainActivity.sortValue = sortMusicEditor.getInt("sortValue", 0)

        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val cursor = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            MainActivity.sortMusicList[MainActivity.sortValue]
        )

        if (cursor != null) {
            if (cursor.moveToNext()) {
                do {
                    val titleMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val idMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                    val pathMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val artistMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val durationMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                            .toLong()
                    val sizeMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val albumMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    val albumIdMC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                            .toString()

                    try {
                        val file = File(pathMC)
                        val albumArtUri =
                            Uri.parse("content://media/external/audio/albumart/$albumIdMC")
                        val music = Music(
                            title = titleMC,
                            id = idMC,
                            duration = durationMC,
                            path = pathMC,
                            artUri = albumArtUri,
                            artist = artistMC,
                            album = albumMC,
                            albumId = albumIdMC,
                            size = sizeMC
                        )


                        if (file.exists()) {
                            tempList.add(music)
                        }

                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "Songs did not load", Toast.LENGTH_SHORT).show()
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        }

        return tempList
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMusic() {
        binding.swipeRefreshMusic.isRefreshing = false



        adapter.notifyDataSetChanged()
    }
    @SuppressLint("SetTextI18n")
    override fun onMusicDeleted() {
        val tempList = getAllAudios()
        adapter.updateMusicList(tempList)

        binding.TotalMusics.text = "Total Musics : ${tempList.size}"
    }


    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_music_view, menu)
        val searchView = menu.findItem(R.id.searchMusicView)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean =true
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null){
                    val userInput = newText.lowercase()
                    for (song in MusicListMA)
                        if (song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    search = true
                    adapter.updateMusicList(searchList = musicListSearch)
                }
                return true
            }
        })


        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
    override fun onResume() {
        super.onResume()
        if (MainActivity.adapterChanged) adapter.notifyDataSetChanged()
        MainActivity.adapterChanged= false


    }




}
