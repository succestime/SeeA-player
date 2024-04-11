

package com.jaidev.seeaplayer


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jaidev.seeaplayer.MainActivity.Companion.MusicListMA
import com.jaidev.seeaplayer.MainActivity.Companion.musicListSearch
import com.jaidev.seeaplayer.MainActivity.Companion.search
import com.jaidev.seeaplayer.databinding.FragmentMusicNavBinding


class musicNav : Fragment(),MusicAdapter.MusicDeleteListener  {

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
        //  FavoritesManager.loadFavorites(requireContext())
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
            startActivity(Intent(requireContext(), PlaylistActivity::class.java))
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

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMusic() {
        binding.swipeRefreshMusic.isRefreshing = false



        adapter.notifyDataSetChanged()
    }
    @SuppressLint("SetTextI18n")
    override fun onMusicDeleted() {
        val mainActivity = requireActivity() as MainActivity
        val tempList = mainActivity.getAllAudios()
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
