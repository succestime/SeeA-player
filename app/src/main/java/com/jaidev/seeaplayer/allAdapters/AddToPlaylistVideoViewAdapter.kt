package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.PlaylistDao
import com.jaidev.seeaplayer.dataClass.PlaylistVideo
import com.jaidev.seeaplayer.dataClass.PlaylistVideoCrossRef
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.PlaylistVideoViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddToPlaylistVideoViewAdapter(private val context: Context,
                                    private var playlists: MutableList<PlaylistVideo>,
                                    private val selectedMusic: VideoData?,
                                    private val bottomSheetPLDialog: BottomSheetDialog,
                                    private val selectedSongs: List<VideoData>
) :
    RecyclerView.Adapter<AddToPlaylistVideoViewAdapter.PlaylistViewHolder>() {

    private val dao: PlaylistDao = DatabaseClient.getInstance(context).playlistDao()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistVideoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding, context, dao, bottomSheetPLDialog, selectedMusic, selectedSongs)
    }



    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size



    fun isEmpty(): Boolean = playlists.isEmpty()



    class PlaylistViewHolder(
        private val binding: PlaylistVideoViewBinding,
        private val context: Context,
        private val dao: PlaylistDao,
        private val bottomSheetPLDialog: BottomSheetDialog,
        private val selectedMusic: VideoData?,
        private val selectedSongs: List<VideoData>
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("StringFormatInvalid", "SetTextI18n")
        fun bind(playlist: PlaylistVideo) {
            binding.playlistName.text = playlist.name

            // Fetch and display the video count and total duration
            binding.root.post {
                CoroutineScope(Dispatchers.IO).launch {
                    val sortOrder = dao.getSortOrder(playlist.id)

                    val videoCount = dao.getVideoCountForPlaylist(playlist.id)
                    val totalDurationMillis =
                        if (videoCount > 0) dao.getTotalDurationForPlaylist(playlist.id) else 0
                    val totalDurationFormatted =
                        if (totalDurationMillis > 0) formatDuration(totalDurationMillis) else ""
                    val firstVideoImageUri = dao.getFirstVideoImageUri(playlist.id, sortOrder)

                    withContext(Dispatchers.Main) {
                        binding.totalVideoContain.text = if (videoCount > 0) {
                            "$videoCount videos • $totalDurationFormatted"
                        } else {
                            "$videoCount videos"
                        }
                        Glide.with(context)
                            .load(firstVideoImageUri)
                            .placeholder(R.color.place_holder_video)
                            .error(R.drawable.play_rectangle_icon)
                            .apply(RequestOptions().transform(CenterInside())) // This ensures that the error image is centered inside
                            .into(binding.playlistImage)

                    }
                }
            }


            binding.root.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val musicToAdd = selectedMusic?.let { listOf(it) } ?: selectedSongs
                    val alreadyInPlaylist = musicToAdd.all { dao.isVideoInPlaylist(playlist.id, it.id) }

                    if (!alreadyInPlaylist) {
                        musicToAdd.forEach { video ->
                            dao.insertPlaylistVideoCrossRef(
                                PlaylistVideoCrossRef(
                                    playlistId = playlist.id,
                                    videoId = video.id
                                )
                            )
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${musicToAdd.size} video(s) added to the playlist'${playlist.name}'",
                                Toast.LENGTH_SHORT
                            ).show()
                            bottomSheetPLDialog.dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${musicToAdd.size} already exist in playlist '${playlist.name}'",
                                Toast.LENGTH_SHORT
                            ).show()
                            bottomSheetPLDialog.dismiss()
                        }
                    }
                }
            }

        }



        private fun formatDuration(durationMillis: Long): String {
            val seconds = (durationMillis / 1000) % 60
            val minutes = (durationMillis / (1000 * 60)) % 60
            val hours = (durationMillis / (1000 * 60 * 60)) % 24

            return buildString {
                if (hours > 0) {
                    append("$hours hrs")
                    if (minutes > 0) append(", $minutes mins")
                } else if (minutes > 0) {
                    append("$minutes mins")
                    if (seconds > 0) append(", $seconds secs")
                } else {
                    append("$seconds secs")
                }
            }
        }

    }
}
