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
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jaidev.seeaplayer.MainActivity.Companion.MusicListMA
import com.jaidev.seeaplayer.MainActivity.Companion.musicListSearch
import com.jaidev.seeaplayer.MainActivity.Companion.search
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.databinding.FragmentMusicNavBinding
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistActivity

class musicNav : Fragment(), MusicAdapter.MusicDeleteListener {

    private lateinit var binding: FragmentMusicNavBinding
    lateinit var adapter: MusicAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetTextI18n", "MissingInflatedId", "NotifyDataSetChanged", "ResourceType")
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

        binding.swipeRefreshMusic.setOnRefreshListener {
            binding.swipeRefreshMusic.isRefreshing = false
        }

        updateEmptyState()

        binding.playandshuffleBtn.setOnClickListener { view ->
            showPlayShuffleMenu(view)
        }

        binding.favouriteBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FavouriteActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        }
        binding.playlistBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PlaylistActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        }

        swipeRefreshLayout = binding.swipeRefreshMusic
        setSwipeRefreshBackgroundColor()

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMusicList() {
        val updatedMusicList = ((activity as MainActivity).getAllAudios()) // Implement this method to fetch the updated music list
        if (updatedMusicList.size != MusicListMA.size) {
            MusicListMA.clear()
            MusicListMA.addAll(updatedMusicList)
            adapter.updateMusicList(MusicListMA)
adapter.notifyDataSetChanged()
            if (MusicListMA.isEmpty()) {
                binding.musicemptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.musicemptyStateLayout.visibility = View.GONE
            }
        }
    }

    private fun showPlayShuffleMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, 0, 0, R.style.CustomPopupMenu)
        popupMenu.inflate(R.menu.play_shuffle_menu)
        try {
            val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldPopup.isAccessible = true
            val popup = fieldPopup.get(popupMenu)
            popup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(popup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play -> {
                    val intent = Intent(requireContext(), PlayerMusicActivity::class.java)
                    intent.putExtra("index", 0)
                    intent.putExtra("class", "MusicNav2")
                    startActivity(intent)
                    true
                }
                R.id.action_shuffle -> {
                    val intent = Intent(requireContext(), PlayerMusicActivity::class.java)
                    intent.putExtra("index", 0)
                    intent.putExtra("class", "MusicNav")
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
        if (isDarkMode) {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onMusicDeleted() {
        refreshMusicList()
    }

    private fun updateEmptyState() {
        if (MusicListMA.isEmpty()) {
            binding.musicemptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.musicemptyStateLayout.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_music_view, menu)
        val searchView = menu.findItem(R.id.searchMusicView)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null) {
                    val userInput = newText.lowercase()
                    for (song in MusicListMA)
                        if (song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    search = true
                    adapter.updateMusicList(musicListSearch)
                }
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation", "SetTextI18n")
    override fun onResume() {
        super.onResume()
        setSwipeRefreshBackgroundColor()
        if (MainActivity.adapterChanged) adapter.notifyDataSetChanged()
        MainActivity.adapterChanged = false
        updateEmptyState()
    }
}
