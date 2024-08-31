package com.jaidev.seeaplayer.allAdapters


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
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
import java.io.File

class AddToPlaylistViewAdapter(
    private val context: Context,
    private var playlists: MutableList<PlaylistMusic>,
    private val selectedMusic: Music?,
    private val bottomSheetPLDialog: BottomSheetDialog,
    private val selectedSongs: List<Music>
) : RecyclerView.Adapter<AddToPlaylistViewAdapter.PlaylistViewHolder>() {

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

            binding.root.post {
                CoroutineScope(Dispatchers.IO).launch {
                    val sortOrder = dao.getSortOrder(playlist.id)
                    val videoCount = dao.getMusicCountForPlaylist(playlist.id)
                    val totalDurationMillis = if (videoCount > 0) dao.getTotalDurationForPlaylist(playlist.id) else 0
                    val totalDurationFormatted = if (totalDurationMillis > 0) formatDuration(totalDurationMillis) else ""
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
                                    .placeholder(R.color.gray)
                                    .error(R.drawable.music_note_svgrepo_com)
                                    .centerCrop()
                            )
                            .into(binding.playlistImage)
                    }
                }
            }

            binding.root.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val musicToAdd = selectedMusic?.let { listOf(it) } ?: selectedSongs

                    // Log the music details
                    musicToAdd.forEach { music ->
                        Log.d("AddToPlaylistViewAdapter", "Checking music ID: ${music.id}, Path: ${music.path}")
                    }

                    // Validate Music and Playlist exist before inserting into cross-reference
                    val validMusicIds = musicToAdd.filter { music ->
                        val file = File(music.path)
                        if (!file.exists()) {
                            Log.e("AddToPlaylistViewAdapter", "File does not exist at path: ${music.path}")
                            false
                        } else {
                            dao.getMusicById(music.id) != null  // Check if the music exists in the database
                        }
                    }.map { it.id }

                    if (validMusicIds.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Cannot add songs because they do not exist in the music library.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    val alreadyInPlaylist = validMusicIds.all { dao.isMusicInPlaylist(playlist.id, it) }

                    if (!alreadyInPlaylist) {
                        validMusicIds.forEach { musicId ->
                            dao.insertPlaylistMusicCrossRef(
                                PlaylistMusicCrossRef(
                                    playlistMusicId = playlist.id,
                                    musicId = musicId
                                )
                            )
                        }

                        // Example of using getFileProviderUri function
                        selectedMusic?.let { music ->
                            val fileUri = getFileProviderUri(music.path)
                            fileUri?.let {
                                // Handle the Uri (e.g., share the file or use it in another activity)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Music"))
                            }
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${validMusicIds.size} song(s) added to the playlist '${playlist.name}'",
                                Toast.LENGTH_SHORT
                            ).show()
                            bottomSheetPLDialog.dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${validMusicIds.size} already exist in playlist '${playlist.name}'",
                                Toast.LENGTH_SHORT
                            ).show()
                            bottomSheetPLDialog.dismiss()
                        }
                    }
                }
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        private fun getFileProviderUri(filePath: String): Uri? {
            return try {
                val file = File(filePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
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
