
package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.dataClass.DatabaseClientMusic
import com.jaidev.seeaplayer.dataClass.PlaylistMusicDao
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.PlaylistMusicMenuBottomSheetBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MorePlaylistMusicBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: PlaylistMusicMenuBottomSheetBinding? = null
    private val binding get() = _binding!!

private lateinit var playlistMusicDao : PlaylistMusicDao
    private var playlistMusicId: Long = 0L
    companion object {
        private const val ARG_PLAYLIST_MUSIC_ID = "playlist_id"

        fun newInstance(playlistMusicId: Long): MorePlaylistMusicBottomSheetFragment {
            val fragment = MorePlaylistMusicBottomSheetFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAYLIST_MUSIC_ID, playlistMusicId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val database = DatabaseClientMusic.getInstance(requireContext())
        playlistMusicDao = database.playlistMusicDao()

        // Retrieve the playlist ID from the arguments
        arguments?.let {
            playlistMusicId = it.getLong(ARG_PLAYLIST_MUSIC_ID)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaylistMusicMenuBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(requireContext())
        ThemeHelper.applyTheme(requireContext(), theme)

        lifecycleScope.launch {
            val playlist = playlistMusicDao.getPlaylistWithMusic(playlistMusicId)
            val sortOrder = playlistMusicDao.getSortOrder(playlistMusicId)

            // Set the playlist folder name
            binding.musicTitle.text = playlist.playlistMusic.name

            // Set the total video count
            val videoCount = playlist.music.size
            binding.totalMusicTitle.text = "$videoCount musics"

            // Set the thumbnail image for the first video
            val firstVideoUri = playlistMusicDao.getFirstMusicImageUri(playlistMusicId , sortOrder)
            Glide.with(this@MorePlaylistMusicBottomSheetFragment)
                .load(firstVideoUri)
                .apply(
                    RequestOptions()
                        .placeholder(R.color.gray) // Use the newly created drawable
                        .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                        .centerCrop()
                )
                .into(binding.musicThumbnail)
        }


        // Handle click events for the options in the bottom sheet
        binding.playOptionLayout.setOnClickListener {
            val addVideosBottomSheetFragment = AddMusicBottomSheetFragment.newInstance(playlistMusicId)
            addVideosBottomSheetFragment.show(parentFragmentManager, addVideosBottomSheetFragment.tag)
            dismiss()
        }

        binding.removeOptionLayout.setOnClickListener {
            // Inflate the custom alert dialog layout
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.playlist_video_all_remove, null)

            // Create the AlertDialog and set the custom view
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            // Find the buttons in the custom layout
            val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
            val removeButton = dialogView.findViewById<Button>(R.id.remove_button)

            // Set up the Cancel button click listener
            cancelButton.setOnClickListener {
                alertDialog.dismiss() // Dismiss the dialog
            }

            removeButton.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        playlistMusicDao.deleteAllMusicFromPlaylist(playlistMusicId)
                    }
                }

                // Send a broadcast to notify that the playlist has been updated
                val intent = Intent("UPDATE_PLAYLIST_MUSIC")
                intent.putExtra("playlistId", playlistMusicId)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
                // Send a broadcast to notify that the playlist has been updated
                val intentFolder = Intent("UPDATE_PLAYLIST_MUSIC")
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intentFolder)

                alertDialog.dismiss()
                dismiss()
            }



            // Show the alert dialog
            alertDialog.show()
        }


        // Handle other options similarly...
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


} 