
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.PlaylistDao
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.PlaylistVideoMenuBottomSheetBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MorePlaylistBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: PlaylistVideoMenuBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var playlistDao: PlaylistDao
    private var playlistId: Long = 0L
    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: Long): MorePlaylistBottomSheetFragment {
            val fragment = MorePlaylistBottomSheetFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAYLIST_ID, playlistId)
            }
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val database = DatabaseClient.getInstance(requireContext())
        playlistDao = database.playlistDao()

        // Retrieve the playlist ID from the arguments
        arguments?.let {
            playlistId = it.getLong(ARG_PLAYLIST_ID)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaylistVideoMenuBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(requireContext())
        ThemeHelper.applyTheme(requireContext(), theme)

        lifecycleScope.launch {
            val playlist = playlistDao.getPlaylistWithVideos(playlistId)

            // Set the playlist folder name
            binding.videoTitle.text = playlist.playlist.name

            // Set the total video count
            val videoCount = playlist.videos.size
            binding.totalVideoTitle.text = "$videoCount videos"

            // Set the thumbnail image for the first video
            val firstVideoUri = playlistDao.getFirstVideoImageUri(playlistId)
            Glide.with(this@MorePlaylistBottomSheetFragment)
                .load(firstVideoUri)
                .centerCrop()
                .into(binding.videoThumbnail)
        }
        // Handle click events for the options in the bottom sheet
        binding.playOptionLayout.setOnClickListener {
            val addVideosBottomSheetFragment = AddVideosBottomSheetFragment.newInstance(playlistId)
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

            // Set up the Remove All button click listener
            removeButton.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        playlistDao.deleteAllVideosFromPlaylist(playlistId)
                    }
                }

                // Send a broadcast to notify that the playlist has been updated
                val intent = Intent("UPDATE_PLAYLIST_VIDEOS")
                intent.putExtra("playlistId", playlistId)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
                // Send a broadcast to notify that the playlist has been updated
                val intentFolder = Intent("UPDATE_PLAYLIST_FOLDER")
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