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
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.MainActivity.Companion.MusicListMA
import com.jaidev.seeaplayer.MainActivity.Companion.musicListSearch
import com.jaidev.seeaplayer.MainActivity.Companion.musicRecantList
import com.jaidev.seeaplayer.MainActivity.Companion.search
import com.jaidev.seeaplayer.Subscription.SeeAOne
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.databinding.FragmentMusicNavBinding
import com.jaidev.seeaplayer.musicActivity.FavouriteActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistActivity

class musicNav : Fragment(), MusicAdapter.MusicDeleteListener ,  RecantMusicAdapter.MusicDeleteListener ,    RecantMusicAdapter.OnFileCountChangeListener
 {

    lateinit var adapter: MusicAdapter
    private lateinit var adapterf: RecantMusicAdapter


    companion object{
        @SuppressLint("StaticFieldLeak")
        private lateinit var binding: FragmentMusicNavBinding

        fun updateEmptyState() {
            if (MusicListMA.isEmpty()) {
                binding.musicemptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.musicemptyStateLayout.visibility = View.GONE
            }
        }
    }
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
        setupActionBar()

        binding.musicRV.setHasFixedSize(true)
        binding.musicRV.setItemViewCacheSize(15)
        binding.musicRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = MusicAdapter(requireContext(), MusicListMA, isMusic = true)
        adapterf = RecantMusicAdapter(requireContext(), musicRecantList, isReMusic = true, this@musicNav, isMusic = false)
        adapter.setMusicDeleteListener(this)
        adapterf.setMusicDeleteListener(this)
        binding.musicRV.adapter = adapter
        refreshMusicList()

        binding.swipeRefreshMusic.setOnRefreshListener {
            refreshMusicList()
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

        return view
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMusicList() {
        val updatedMusicList = ((activity as MainActivity).getAllAudio()) // Implement this method to fetch the updated music list
        if (updatedMusicList.size != MusicListMA.size) {
            MusicListMA.clear()
            MusicListMA.addAll(updatedMusicList)
            adapter.updateMusicList(MusicListMA)
             adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }
    private fun setupActionBar() {
        val inflater = LayoutInflater.from(requireContext())
        val customActionBarView = inflater.inflate(R.layout.custom_action_bar_layout, null)

        val titleTextView = customActionBarView.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = "Musics"

        val subscribeTextView = customActionBarView.findViewById<TextView>(R.id.subscribe)
        if (MainActivity.isInternetAvailable(requireContext())) {
            subscribeTextView.visibility = View.VISIBLE
            subscribeTextView.setOnClickListener {
                startActivity(Intent(requireContext(), SeeAOne::class.java))
                (activity as AppCompatActivity).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
            }
        } else {
            subscribeTextView.visibility = View.GONE
        }


        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
            customView = customActionBarView
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


                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }



    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged")
    override fun onMusicDeleted() {
        refreshMusicList()
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
        updateEmptyState()

    }

     override fun onFileCountChanged(newCount: Int) {
         TODO("Not yet implemented")
     }


 }
