package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.allAdapters.MusicFavAdapter2
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavEntity
import com.jaidev.seeaplayer.databinding.FavMusicSelectionBottomSheetBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddMusicFavBottomSheetFragment : BottomSheetDialogFragment(), MusicFavAdapter2.OnSelectionChangeListener{

    private lateinit var _binding: FavMusicSelectionBottomSheetBinding


    private val binding get() = _binding
    private lateinit var adapter: MusicFavAdapter2
    private lateinit var favoriteSongs: ArrayList<Music>

    companion object {
        private const val ARG_FAVORITE_SONGS = "favorite_songs"

        fun newInstance(favoriteSongs: ArrayList<Music>): AddMusicFavBottomSheetFragment {
            val fragment = AddMusicFavBottomSheetFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_FAVORITE_SONGS, favoriteSongs)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FavMusicSelectionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoriteSongs = arguments?.getParcelableArrayList(ARG_FAVORITE_SONGS) ?: ArrayList()

        // Initialize RecyclerView with LayoutManager
        binding.playlistSelectionVideoRV.layoutManager = LinearLayoutManager(context)
        binding.playlistSelectionVideoRV.setHasFixedSize(true)
        binding.playlistSelectionVideoRV.setItemViewCacheSize(15)

        lifecycleScope.launch {

            // Ensure UI updates are done on the main thread
            withContext(Dispatchers.Main) {
                adapter = MusicFavAdapter2(requireContext(), MainActivity.MusicListMA)
                binding.playlistSelectionVideoRV.adapter = adapter
                adapter.setOnSelectionChangeListener(this@AddMusicFavBottomSheetFragment)

                updateAddSelectedVideoButtonVisibility()
            }
        }



        binding.clearBottomSheet.setOnClickListener {
            dismiss()
        }
        binding.addSelectedVideo.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val musicFavDao = MusicFavDatabase.getDatabase(requireContext()).musicFavDao()
                val existingFavs = musicFavDao.getAllMusic()
                val selectedItems = adapter.getSelectedVideos()

                // Filter out songs that are already in favorites
                val newFavorites = selectedItems.filter { selectedSong ->
                    existingFavs.none { favSong -> favSong.musicid == selectedSong.musicid }
                }

                if (newFavorites.isNotEmpty()) {
                    // Start a manual transaction
                    MusicFavDatabase.getDatabase(requireContext()).beginTransaction()
                    try {
                        for (song in newFavorites) {
                            val musicFavEntity = MusicFavEntity(
                                musicid = song.musicid,
                                title = song.title,
                                album = song.album,
                                artist = song.artist,
                                duration = song.duration,
                                path = song.path,
                                size = song.size,
                                artUri = song.artUri,
                                dateAdded = song.dateAdded
                            )
                            musicFavDao.insertMusic(musicFavEntity)
                        }
                        // Mark the transaction as successful
                        MusicFavDatabase.getDatabase(requireContext()).setTransactionSuccessful()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error adding songs to favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        // End the transaction
                        MusicFavDatabase.getDatabase(requireContext()).endTransaction()
                    }

                    withContext(Dispatchers.Main) {
                        sendFavoritesUpdatedBroadcast()
                        Toast.makeText(requireContext(), "Added ${newFavorites.size} song(s) to favorites", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        sendFavoritesUpdatedBroadcast()
                        Toast.makeText(requireContext(), "All selected song(s) are already favorites", Toast.LENGTH_SHORT).show()
                    }
                }

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    dismiss()
                }
            }
        }

        // Set up SearchView to filter items in the adapter
        binding.searchViewSA.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

       updateAddSelectedVideoButtonVisibility()

    }
    override fun onSelectionChanged(count: Int) {
        activity?.runOnUiThread {
            updateAddSelectedVideoButtonVisibility()
            updateTotalVideoSelectedText(count)
        }
    }
    private fun sendFavoritesUpdatedBroadcast() {
        val intent = Intent("com.yourpackage.FAVORITES_UPDATED")
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTotalVideoSelectedText(count: Int) {
        binding.totalVideoSelected.text = "Add to Favourites ($count ${if (count == 1) "song" else "song" +
                "s"})"
    }
    private fun updateAddSelectedVideoButtonVisibility() {
        if (this::adapter.isInitialized && adapter.getSelectedItemCount() > 0) {
            binding.addSelectedVideo.visibility = View.VISIBLE
        } else {
            binding.addSelectedVideo.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {

            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

}
