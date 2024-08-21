package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavEntity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistDetails
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaylistMusicShowAdapter(
    private val context: Context,
    private var videoList: ArrayList<Music>,
    private val onVideoRemoved: (Music) -> Unit
) : RecyclerView.Adapter<PlaylistMusicShowAdapter.VideoViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    private var isAllSelected = false // Add this flag

    var isSelectionMode = false
    interface OnSelectionChangeListener {

        fun onSelectionChanged(isAllSelected: Boolean)
    }

    var selectionChangeListener: OnSelectionChangeListener? = null

    fun getVideos(): ArrayList<Music> {
        return videoList
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateVideoList(newFavourites: ArrayList<Music>) {
    videoList = newFavourites
    notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = true
        notifyDataSetChanged()
        selectionChangeListener?.onSelectionChanged(isAllSelected = false)

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistDetails
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(0)
    }
    fun getSelectedVideos(): ArrayList<Music> {
        return ArrayList(videoList.filter { it.selected })
    }
    // Inside PlaylistVideoShowAdapter class

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllVideos(select: Boolean) {
        selectedItems.clear()
        if (select) {
            videoList.forEachIndexed { index, video ->
                video.selected = true
                selectedItems.add(index)
            }
        } else {
            videoList.forEach { video ->
                video.selected = false
            }
        }
        notifyDataSetChanged()
        isSelectionMode = true
        val activity = context as PlaylistDetails
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(selectedItems.size)
        selectionChangeListener?.onSelectionChanged(select)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSelectionMode() {
        val activity = context as PlaylistDetails

        // Update the playlist name to show the number of selected items
        val selectedCount = selectedItems.size
        activity.updatePlaylistName(selectedCount)

        // Keep the selection mode active even if no items are selected
        isSelectionMode = true
        activity.updateSelectionMode(true)

        // Trigger UI updates
        notifyDataSetChanged()

        // Notify the listener about the selection state
        selectionChangeListener?.onSelectionChanged(selectedCount == videoList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistDetails
        activity.updateSelectionMode(false)
        activity.updatePlaylistName(0) // Set to "0 videos selected"
    }


    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.videoName)
        val albumName: TextView = itemView.findViewById(R.id.albumName)
        val videoImage: ShapeableImageView = itemView.findViewById(R.id.videoImage)
        val moreChoose: ImageButton = itemView.findViewById(R.id.MoreChoose)
        val emptyCheck: ImageButton = itemView.findViewById(R.id.emptyCheck)
        val fillCheck: ImageButton = itemView.findViewById(R.id.fillCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_music_music_view, parent, false)
        return VideoViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.titleTextView.text = video.title
        holder.albumName.text = video.album
        Glide.with(context)
            .load(video.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                    .centerCrop()
            )
            .into(holder.videoImage)
        // If selection mode is enabled
        if (isSelectionMode) {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.moreChoose.visibility = View.GONE

            // If the item is selected
            if (selectedItems.contains(position)) {
                holder.fillCheck.visibility = View.VISIBLE
                holder.emptyCheck.visibility = View.GONE
            } else {
                holder.fillCheck.visibility = View.GONE
                holder.emptyCheck.visibility = View.VISIBLE
            }
        } else {
            // If selection mode is not enabled
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.GONE
            holder.moreChoose.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                val intent = Intent(context, PlayerMusicActivity::class.java)
                intent.putExtra("index", position)
                intent.putExtra("class", "FavouriteAShuffle")
                ContextCompat.startActivity(context, intent, null)
            }
        }

        // Handle long click to enable selection mode
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                updateSelectionMode()
                notifyDataSetChanged() // Refresh to show/hide checkboxes for all items
            }
            toggleSelection(position)
            true
        }

        // Handle more button click only when not in selection mode
        holder.moreChoose.setOnClickListener {
            if (!isSelectionMode) {
                showBottomSheetDialog(video , position)
            }
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        val video = videoList[position]
        video.selected = !video.selected
        notifyItemChanged(position)
        updateSelectionMode()
    }


    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "ObsoleteSdkInt")
    private fun showBottomSheetDialog(video: Music , position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.playlist_music_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        view.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        val videoThumbnail: ShapeableImageView = view.findViewById(R.id.videoThumbnail)
        val duration: TextView = view.findViewById(R.id.duration)
        val albumTitle: TextView = view.findViewById(R.id.albumTitle)
        val favouritesIcon: ImageView = view.findViewById(R.id.favsIcon)
        val addToFavTitle: TextView = view.findViewById(R.id.addToFavTitle)

        albumTitle.text = video.album
        duration.text = DateUtils.formatElapsedTime(video.duration / 1000)
        videoTitle.text = video.title
        Glide.with(context)
            .load(video.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                    .centerCrop()
            )
            .into(videoThumbnail)



        val playOptionLayout: LinearLayout = view.findViewById(R.id.playOptionLayout)
        val addToFavLayout: LinearLayout = view.findViewById(R.id.addToFavLayout)
        val setAsRingtoneLayout: LinearLayout = view.findViewById(R.id.setAsRingtoneLayout)
        val shareLayout: LinearLayout = view.findViewById(R.id.shareLayout)
        val removeOptionLayout: LinearLayout = view.findViewById(R.id.removeOptionLayout)
        val propertiesOptionLayout: LinearLayout = view.findViewById(R.id.propertiesOptionLayout)

        // Fetch the MusicFavDao once
        val musicDao = MusicFavDatabase.getDatabase(context).musicFavDao()

        // Check if the song is already in favorites and update UI
        GlobalScope.launch(Dispatchers.Main) {
            val isFavorite = withContext(Dispatchers.IO) {
                musicDao.getAllMusic().any { it.id == video.id }
            }

            if (isFavorite) {
                favouritesIcon.setImageResource(R.drawable.round_favorite_music)
                addToFavTitle.text = "Added to Favourites"
            } else {
                favouritesIcon.setImageResource(R.drawable.round_favorite_border_music)
                addToFavTitle.text = "Add to Favourites"
            }

            addToFavLayout.setOnClickListener {
                GlobalScope.launch(Dispatchers.IO) {
                    if (isFavorite) {
                        musicDao.deleteMusic(
                            MusicFavEntity(
                                id = video.id,
                                title = video.title,
                                album = video.album,
                                artist = video.artist,
                                duration = video.duration,
                                path = video.path,
                                size = video.size,
                                artUri = video.artUri,
                                dateAdded = video.dateAdded
                            )
                        )
                    } else {
                        musicDao.insertMusic(
                            MusicFavEntity(
                                id = video.id,
                                title = video.title,
                                album = video.album,
                                artist = video.artist,
                                duration = video.duration,
                                path = video.path,
                                size = video.size,
                                artUri = video.artUri,
                                dateAdded = video.dateAdded
                            )
                        )
                    }
                }
                bottomSheetDialog.dismiss()
            }
        }


        setAsRingtoneLayout.setOnClickListener {
            val builderTone = AlertDialog.Builder(context)
            val dialogViewTone =
                LayoutInflater.from(context).inflate(R.layout.favurite_ringtone, null)
            builderTone.setView(dialogViewTone)

            val dialogTone = builderTone.create()

            val notButton: Button = dialogViewTone.findViewById(R.id.not_button)
            val yesButton: Button = dialogViewTone.findViewById(R.id.yes_button)

            notButton.setOnClickListener {
                dialogTone.dismiss()
            }

            yesButton.setOnClickListener {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (!android.provider.Settings.System.canWrite(context)) {
                        // Request permission
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:" + context.packageName)
                        ContextCompat.startActivity(context, intent, null)
                    } else {
                        setRingtone(video.path)
                    }
                } else {
                    setRingtone(video.path)
                }
                dialogTone.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialogTone.show()
            bottomSheetDialog.dismiss()
        }

        shareLayout.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "audio/*" // This sets the MIME type to audio
                putExtra(Intent.EXTRA_STREAM, Uri.parse(video.path))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission for the receiving app to read the URI
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Playlist Song"))
            bottomSheetDialog.dismiss()
        }

        playOptionLayout.setOnClickListener {
            val intent = Intent(context, PlayerMusicActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "bottomSheetPlay")
            ContextCompat.startActivity(context, intent, null)
            bottomSheetDialog.dismiss()


        }

        removeOptionLayout.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.playlist_remove, null)
            builder.setView(dialogView)

            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeButton.setOnClickListener {
                onVideoRemoved(video)
                dialog.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialog.show()
            bottomSheetDialog.dismiss()
        }

        propertiesOptionLayout.setOnClickListener {
            val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
            val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
            val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
            val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
            val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
            val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)
            val dateTextView = customDialogIF.findViewById<TextView>(R.id.dateDetail)

            fileNameTextView.text = video.title
            durationTextView.text = DateUtils.formatElapsedTime(video.duration / 1000)
            val sizeInBytes = video.size.toLong()
            val sizeInMb = sizeInBytes / (1024 * 1024)
            val formattedSizeInMb = String.format("%.1fMB", sizeInMb.toDouble())
            val formattedSizeInBytes = String.format("(%,d bytes)", sizeInBytes)
            sizeTextView.text = "$formattedSizeInMb $formattedSizeInBytes"
            locationTextView.text = video.path

            val dateFormat = SimpleDateFormat("MMMM d, yyyy, HH:mm", Locale.getDefault())
            val formattedDate = video.dateAdded?.let { Date(it) }?.let { dateFormat.format(it) }
            dateTextView.text = formattedDate ?: "Unknown date"

            val dialogIF = MaterialAlertDialogBuilder(context)
                .setView(customDialogIF)
                .setCancelable(false)
                .create()
            positiveButton.setOnClickListener {
                dialogIF.dismiss()
            }
            dialogIF.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
    private fun setRingtone(filePath: String) {
        val file = File(filePath)
        val contentUri = Uri.fromFile(file)

        if (contentUri != null) {
            val resolver = context.contentResolver
            val ringtoneUri = MediaStore.Audio.Media.getContentUriForPath(filePath)

            // Check if the file is already in the MediaStore
            val cursor = resolver.query(
                ringtoneUri!!,
                arrayOf(MediaStore.MediaColumns._ID),
                MediaStore.MediaColumns.DATA + "=?",
                arrayOf(file.absolutePath),
                null
            )

            var uri: Uri? = null

            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                uri = ContentUris.withAppendedId(ringtoneUri, id)
            } else {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DATA, file.absolutePath)
                contentValues.put(MediaStore.MediaColumns.TITLE, file.name)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, true)
                contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                contentValues.put(MediaStore.Audio.Media.IS_ALARM, false)
                contentValues.put(MediaStore.Audio.Media.IS_MUSIC, false)

                // Insert the ringtone into the media store
                uri = resolver.insert(ringtoneUri, contentValues)
            }

            cursor?.close()

            // Set the ringtone
            if (uri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
                Toast.makeText(context, "Ringtone set successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to set ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = videoList.size
}
