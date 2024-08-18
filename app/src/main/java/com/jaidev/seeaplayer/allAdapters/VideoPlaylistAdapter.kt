package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.PlaylistVideoActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.PlaylistDao
import com.jaidev.seeaplayer.dataClass.PlaylistVideo
import com.jaidev.seeaplayer.databinding.PlaylistVideoViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PlaylistAdapter(private val context: Context, private var playlists: MutableList<PlaylistVideo> ) :
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    private val dao: PlaylistDao = DatabaseClient.getInstance(context).playlistDao()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistVideoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding, context, dao, this)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    fun addPlaylist(playlist: PlaylistVideo) {
        playlists.add(playlist)
        notifyItemInserted(playlists.size - 1)
    }

    fun isEmpty(): Boolean = playlists.isEmpty()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePlaylists(playlists: List<PlaylistVideo>) {
        this.playlists = playlists.toMutableList()
        notifyDataSetChanged()
    }


    class PlaylistViewHolder(
        private val binding: PlaylistVideoViewBinding,
        private val context: Context,
        private val dao: PlaylistDao,
        private val adapter: PlaylistAdapter
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
                val intent = Intent(context, PlaylistVideoActivity::class.java).apply {
                    putExtra("playlistId", playlist.id)
                    PlaylistVideoActivity.videoList.clear()
                }
                context.startActivity(intent)
                (context as Activity).overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_right
                )
            }

            binding.playlistMore.setOnClickListener {
                showBottomSheetDialog(playlist)
            }
        }

        @SuppressLint("SetTextI18n", "InflateParams")
        private fun showBottomSheetDialog(playlist: PlaylistVideo) {
            val bottomSheetDialog = BottomSheetDialog(context)
            val bottomSheetView = LayoutInflater.from(context).inflate(
                R.layout.playlist_folder_bottom_sheet, null
            )
            bottomSheetDialog.setContentView(bottomSheetView)

            val imageThumbnail = bottomSheetView.findViewById<ShapeableImageView>(R.id.imageThumbnail)
            val textTitle = bottomSheetView.findViewById<TextView>(R.id.textTitle)
            val textSubtitle = bottomSheetView.findViewById<TextView>(R.id.textSubtitle)
            val deleteButton = bottomSheetView.findViewById<LinearLayout>(R.id.deleteButton)
            val renameButton = bottomSheetView.findViewById<LinearLayout>(R.id.renameButton)
            val playButton = bottomSheetView.findViewById<LinearLayout>(R.id.playButton)

            textTitle.text = playlist.name

            // Launch a coroutine to call the suspend functions
            CoroutineScope(Dispatchers.IO).launch {
                val sortOrder = dao.getSortOrder(playlist.id)
                val videoCount = dao.getVideoCountForPlaylist(playlist.id)
                val imageUri = dao.getFirstVideoImageUri(playlist.id, sortOrder)

                withContext(Dispatchers.Main) {
                    textSubtitle.text = "$videoCount videos"

                    Glide.with(context)
                        .load(imageUri)
                        .placeholder(R.color.place_holder_video) // Optional placeholder
                        .error(R.drawable.play_rectangle_icon)
                        .into(imageThumbnail)
                }
            }

            playButton.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val videoUris = dao.getVideosForPlaylist(playlist.id) // Fetch the video URIs from the database

                    withContext(Dispatchers.Main) {
                        if (videoUris.isEmpty()) {
                            // Show a toast message if the playlist is empty
                            Toast.makeText(context, "No videos in this playlist", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(context, PlayerFileActivity::class.java).apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videoUris.map { Uri.parse(it) }))
                            }
                            context.startActivity(intent)
                        }
                        bottomSheetDialog.dismiss() // Dismiss the dialog after clicking play
                    }
                }
            }

            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(playlist) // Call the delete dialog function
                bottomSheetDialog.dismiss() // Optionally dismiss the bottom sheet dialog
            }

            renameButton.setOnClickListener {
                showRenameDialog(playlist, adapterPosition)
                bottomSheetDialog.dismiss() // Dismiss the dialog after clicking rename
            }

            bottomSheetDialog.show()
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

        private fun showRenameDialog(playlist: PlaylistVideo, position: Int) {
            lateinit var dialogRF: AlertDialog

            val dialogBuilder = AlertDialog.Builder(context)

            // Set up the layout for the dialog
            val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
            val editText = view.findViewById<EditText>(R.id.renameField)
            editText.setText(playlist.name) // Set default text as current playlist name

            dialogBuilder.setView(view)
                .setTitle("Rename Playlist")
                .setMessage("Enter new name for the playlist:")
                .setCancelable(false)
                .setPositiveButton("Okay") { dialog, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty() && newName != playlist.name) {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.updatePlaylistName(playlist.id, newName)
                            withContext(Dispatchers.Main) {
                                playlist.name = newName
                                adapter.notifyItemChanged(position)
                            }
                        }
                    } else {
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            dialogRF = dialogBuilder.create()
            dialogRF.show()

            // Get the buttons
            val positiveButton = dialogRF.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE)


            // Set the initial state of the buttons
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
            positiveButton.isEnabled = false // Initially disabled

            // Set the negative button color
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.gray))


            // Add a TextWatcher to the EditText to monitor text changes
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val newName = s.toString().trim()
                    if (newName.isNotEmpty() && newName != playlist.name) {
                        positiveButton.setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
                        positiveButton.isEnabled = true
                    } else {
                        positiveButton.setTextColor(ContextCompat.getColor(context, R.color.gray)) // Set to gray color
                        positiveButton.isEnabled = false
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        @SuppressLint("InflateParams")
        private fun showDeleteConfirmationDialog(playlist: PlaylistVideo) {
            // Create an AlertDialog Builder
            val dialogBuilder = AlertDialog.Builder(context)

            // Inflate the custom layout
            val dialogView = LayoutInflater.from(context).inflate(R.layout.playlist_delete_video_dialog, null)
            dialogBuilder.setView(dialogView)

            // Set up the views in the dialog
            val playlistImage = dialogView.findViewById<ShapeableImageView>(R.id.playlist_image)
            val playlistName = dialogView.findViewById<TextView>(R.id.playlist_name)
            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
            val btnDelete = dialogView.findViewById<Button>(R.id.btn_delete)

            // Set playlist details in the dialog
            playlistName.text = playlist.name
            // Load the playlist image using Glide
            CoroutineScope(Dispatchers.IO).launch {
                val sortOrder = dao.getSortOrder(playlist.id)

                val imageUri = dao.getFirstVideoImageUri(playlist.id , sortOrder)
                withContext(Dispatchers.Main) {
                    Glide.with(context)
                        .load(imageUri)
                        .placeholder(R.color.place_holder_video) // Optional placeholder
                        .error(R.drawable.play_rectangle_icon)
                        .into(playlistImage)
                }
            }

            // Create and show the AlertDialog
            val dialog = dialogBuilder.create()
            dialog.show()

            // Set button colors
            btnCancel.setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
            btnDelete.setTextColor(ContextCompat.getColor(context, R.color.red))

            // Handle button clicks
            btnCancel.setOnClickListener {
                dialog.dismiss() // Dismiss the dialog
            }

            btnDelete.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    dao.deletePlaylist(playlist.id)
                    withContext(Dispatchers.Main) {
                        adapter.playlists.removeAt(adapterPosition)
                        adapter.notifyItemRemoved(adapterPosition)
                        dialog.dismiss()
                    }
                }
            }
        }

    }
}
