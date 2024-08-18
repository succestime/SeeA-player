package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.DatabaseClientMusic
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.PlaylistMusic
import com.jaidev.seeaplayer.dataClass.PlaylistMusicCrossRef
import com.jaidev.seeaplayer.dataClass.PlaylistMusicDao
import com.jaidev.seeaplayer.databinding.AddToPlaylistMusicViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddToPlaylistViewAdapter(    private val context: Context,
                                   private var playlists: MutableList<PlaylistMusic>,
                                   private val selectedMusic: Music?,
                                   private val bottomSheetPLDialog: BottomSheetDialog,
                                   private val selectedSongs: List<Music>
) :
    RecyclerView.Adapter<AddToPlaylistViewAdapter.PlaylistViewHolder>() {

    private val dao: PlaylistMusicDao = DatabaseClientMusic.getInstance(context).playlistMusicDao()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = AddToPlaylistMusicViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding, context, dao, bottomSheetPLDialog, selectedMusic, selectedSongs)
    }



    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size



    fun isEmpty(): Boolean = playlists.isEmpty()



    class PlaylistViewHolder(
        private val binding: AddToPlaylistMusicViewBinding,
        private val context: Context,
        private val dao: PlaylistMusicDao,
        private val bottomSheetPLDialog: BottomSheetDialog,
        private val selectedMusic: Music?,
        private val selectedSongs: List<Music>
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("StringFormatInvalid", "SetTextI18n")
        fun bind(playlist: PlaylistMusic) {
            binding.playlistName.text = playlist.name

            // Fetch and display the video count and total duration
            binding.root.post {

                CoroutineScope(Dispatchers.IO).launch {
                    val sortOrder = dao.getSortOrder(playlist.id)

                    val videoCount = dao.getMusicCountForPlaylist(playlist.id)
                    val totalDurationMillis =
                        if (videoCount > 0) dao.getTotalDurationForPlaylist(playlist.id) else 0
                    val totalDurationFormatted =
                        if (totalDurationMillis > 0) formatDuration(totalDurationMillis) else ""
                    val firstVideoImageUri = dao.getFirstMusicImageUri(playlist.id, sortOrder)

                    withContext(Dispatchers.Main) {
                        binding.totalVideoContain.text = if (videoCount > 0) {
                            "$videoCount musics • $totalDurationFormatted"
                        } else {
                            "$videoCount musics"
                        }


                        Glide.with(context)
                            .load(firstVideoImageUri)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.color.gray) // Use the newly created drawable
                                    .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                                    .centerCrop()
                            )
                            .into(binding.playlistImage)

                    }
                }
            }

            binding.root.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val musicToAdd = selectedMusic?.let { listOf(it) } ?: selectedSongs
                    val alreadyInPlaylist = musicToAdd.all { dao.isMusicInPlaylist(playlist.id, it.id) }

                    if (!alreadyInPlaylist) {
                        musicToAdd.forEach { music ->
                            dao.insertPlaylistMusicCrossRef(
                                PlaylistMusicCrossRef(
                                    playlistMusicId = playlist.id,
                                    musicId = music.id
                                )
                            )
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${musicToAdd.size} song(s) added to the playlist'${playlist.name}'",
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
