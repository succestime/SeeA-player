
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.jaidev.seeaplayer.dataClass.PlaylistMusic
import com.jaidev.seeaplayer.dataClass.PlaylistMusicEntity
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.FavouriteViewBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat

class FavouriteAdapter(private val context: Context, private var musicList : ArrayList<Music> ,
                       private val selectionModeChangeListener: OnSelectionModeChangeListener

):
    RecyclerView.Adapter<FavouriteAdapter.MyAdapter>() {
    var isSelectionMode = false
    val selectedItems = mutableSetOf<Int>()
    interface OnSelectionModeChangeListener {
        fun onSelectionModeChanged(isSelectionMode: Boolean, selectedCount: Int, totalCount: Int)
    }
    // In your FavouriteAdapter class
    @SuppressLint("NotifyDataSetChanged")
    fun enableSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged()  // To refresh the UI and show selection checkboxes, if applicable
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size
    }

    fun getSelectedVideos(): List<Music> {
        return selectedItems.map { position -> musicList[position] }
    }
    fun getSelectedSongs(): List<Music> {
        return selectedItems.map { position -> musicList[position] }
    }



    class MyAdapter(binding: FavouriteViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.videoImage
        val name = binding.videoName
        val album = binding.videoAlbum
        val moreChoose = binding.MoreChoose
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(FavouriteViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyAdapter, position: Int) {
        val video = musicList[position]


        holder.name.text = musicList[position].title
        holder.album.text = musicList[position].album
        Glide.with(context)
            .load(getImgArt(video.path))
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com)
                    .centerCrop()
            )
            .into(holder.image)


        // If selection mode is enabled
        if (isSelectionMode ) {
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

        holder.moreChoose.setOnClickListener {
            showBottomSheetDialog(video, position)
        }
        holder.root.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
                notifyItemChanged(position)
            } else {
                when (video.musicid) {
                    PlayerMusicActivity.nowMusicPlayingId ->
                        sendIntent(
                            ref = "FavNowPlaying",
                            pos = PlayerMusicActivity.songPosition
                        )
                    else -> sendIntent(ref = "FavouriteAdapter", pos = position)
                }

            }
        }


        holder.root.setOnLongClickListener {
                isSelectionMode = true
                toggleSelection(position)
            notifyDataSetChanged()
            true
        }
    }

    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, PlayerMusicActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
        (context as Activity).overridePendingTransition(
            R.anim.slide_in_bottom,
            R.anim.anim_no_change // Using a transparent animation for exit
        )
    }
    override fun getItemCount(): Int {
        return musicList.size
    }


    fun isSelected(position: Int): Boolean {
        return selectedItems.contains(position)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selectedItems.clear()
        for (i in musicList.indices) {
            selectedItems.add(i)
        }
        notifyDataSetChanged()
        selectionModeChangeListener.onSelectionModeChanged(true, selectedItems.size, musicList.size)

    }

    @SuppressLint("NotifyDataSetChanged")
    fun deselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
        selectionModeChangeListener.onSelectionModeChanged(true, 0, musicList.size)

    }

    fun toggleSelection(position: Int) {
        if (musicList.isEmpty()) return // Prevent toggling if there are no songs

        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        val selectedCount = selectedItems.size

        // Notify the activity about the selection mode change and whether all items are selected
        selectionModeChangeListener.onSelectionModeChanged(isSelectionMode, selectedCount, musicList.size)

        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        selectionModeChangeListener.onSelectionModeChanged(false, 0, musicList.size)
        notifyDataSetChanged()
    }

    //FavouriteAdapter
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n", "InflateParams", "ObsoleteSdkInt")
    private fun showBottomSheetDialog(playlist: Music, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(
            R.layout.favourite_music_bottom_sheet, null
        )
        bottomSheetDialog.setContentView(bottomSheetView)

        val imageThumbnail = bottomSheetView.findViewById<ShapeableImageView>(R.id.imageThumbnail)
        val textTitle = bottomSheetView.findViewById<TextView>(R.id.textTitle)
        val textSubtitle = bottomSheetView.findViewById<TextView>(R.id.textSubtitle)
        val ringtoneButton = bottomSheetView.findViewById<LinearLayout>(R.id.ringtoneButton)
        val removeButton = bottomSheetView.findViewById<LinearLayout>(R.id.removeButton)
        val playButton = bottomSheetView.findViewById<LinearLayout>(R.id.playButton)
        val shareButton = bottomSheetView.findViewById<LinearLayout>(R.id.shareButton)
        val propertiesButton = bottomSheetView.findViewById<LinearLayout>(R.id.propertiesButton)
        val removeToFavouriteButton = bottomSheetView.findViewById<LinearLayout>(R.id.removeToFavouriteButton)
//        val addToPlaylistButton = bottomSheetView.findViewById<LinearLayout>(R.id.addToPlaylistButton)

        textTitle.text = playlist.title
        textSubtitle.text = playlist.album

        Glide.with(context)
            .load(getImgArt(playlist.path))
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com)
                    .centerCrop()
            )
            .into(imageThumbnail)

//    addToPlaylistButton.setOnClickListener {
//        // Create and show a new BottomSheetDialog for adding to playlist
//        val bottomSheetPLDialog = BottomSheetDialog(context)
//        val bottomSheetPLView = LayoutInflater.from(context).inflate(
//            R.layout.add_to_playlist_bottom_sheet, null
//        )
//        bottomSheetPLDialog.setContentView(bottomSheetPLView)
//
//        val createPlaylistButton = bottomSheetPLView.findViewById<Button>(R.id.create_playlist_button)
//        val playlistRecyclerView = bottomSheetPLView.findViewById<RecyclerView>(R.id.playlistRecyclerview)
//
//        // Set up RecyclerView
//        playlistRecyclerView.layoutManager = LinearLayoutManager(context)
//
//        // Fetch playlists from the database
//        CoroutineScope(Dispatchers.IO).launch {
//            val dao = DatabaseClientMusic.getInstance(context).playlistMusicDao()
//            val playlistEntities = dao.getAllPlaylists() // Fetch PlaylistMusicEntity list
//
//            // Convert PlaylistMusicEntity list to PlaylistMusic list
//            val playlists = playlistEntities.map { mapEntityToPlaylistMusic(it) }
//
//            withContext(Dispatchers.Main) {
//                // Initialize and set the adapter
//                val playlistAdapter = AddToPlaylistViewAdapter(
//                    context,
//                    playlists.toMutableList(),
//                    playlist, // Single song
//                    bottomSheetPLDialog,
//                    selectedSongs = emptyList() // No multiple selection here
//                )
//                playlistRecyclerView.adapter = playlistAdapter
//            }
//        }
//
//        // Handle the create playlist button click
//        createPlaylistButton.setOnClickListener {
//            // Inflate the new bottom sheet layout for creating a playlist
//            val createPlaylistView = LayoutInflater.from(context).inflate(
//                R.layout.video_playlist_bottom_dialog, null
//            )
//
//            val createPlaylistDialog = BottomSheetDialog(context)
//            createPlaylistDialog.setContentView(createPlaylistView)
//
//            // Find the views in the create playlist bottom sheet layout
//            val renameField = createPlaylistView.findViewById<TextInputEditText>(R.id.renameField)
//            val createButton = createPlaylistView.findViewById<Button>(R.id.button_create_playlist)
//
//            renameField.addTextChangedListener(object : TextWatcher {
//                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//                override fun afterTextChanged(s: Editable?) {
//                    createButton.setBackgroundColor(
//                        if (s.isNullOrEmpty()) {
//                            ContextCompat.getColor(context, R.color.button_background_default)
//                        } else {
//                            ContextCompat.getColor(context, R.color.cool_blue)
//                        }
//                    )
//                }
//            })
//
//            createButton.setOnClickListener {
//                val playlistName = renameField.text.toString().trim()
//                if (playlistName.isNotEmpty()) {
//                    GlobalScope.launch(Dispatchers.IO) {
//                        val dao = DatabaseClientMusic.getInstance(context).playlistMusicDao()
//
//                        // Create a new playlist entity
//                        val newPlaylist = PlaylistMusicEntity(
//                            name = playlistName
//                        )
//
//                        // Insert the new playlist into the database and get its ID
//                        val playlistId = dao.insertPlaylist(newPlaylist)
//
//                        // Add the selected song to the newly created playlist
//                        val crossRef = PlaylistMusicCrossRef(
//                            playlistMusicId = playlistId,
//                            musicId = playlist.id // Assuming playlist is of type Music
//                        )
//                        dao.insertPlaylistMusicCrossRef(crossRef)
//
//                        withContext(Dispatchers.Main) {
//                            // Dismiss the dialogs
//                            createPlaylistDialog.dismiss()
//                            bottomSheetPLDialog.dismiss()
//                            val numberOfSongs = 1 // Change this if you are adding multiple songs
//                            Toast.makeText(context, "$numberOfSongs song(s) added to the playlist '$playlistName'", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                } else {
//                    // Handle empty name case (e.g., show an error message)
//                    Toast.makeText(context, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            // Show the create playlist bottom sheet
//            createPlaylistDialog.show()
//            bottomSheetPLDialog.dismiss()
//        }
//
//        bottomSheetPLDialog.show()
//        bottomSheetDialog.dismiss()
//    }

        removeToFavouriteButton.setOnClickListener {
            val removedMusic = musicList[position]

            // Remove from the database
            GlobalScope.launch(Dispatchers.IO) {
                val musicFavDao = MusicFavDatabase.getDatabase(context).musicFavDao()
                val musicFavEntity = MusicFavEntity(
                    musicid = removedMusic.musicid,
                    title = removedMusic.title,
                    album = removedMusic.album,
                    artist = removedMusic.artist,
                    duration = removedMusic.duration,
                    path = removedMusic.path,
                    size = removedMusic.size,
                    artUri = removedMusic.artUri,
                    dateAdded = removedMusic.dateAdded
                )
                musicFavDao.deleteMusic(musicFavEntity)
            }

            // Remove from the list and update UI
            musicList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, musicList.size)

            Toast.makeText(context, "${removedMusic.title} removed from favorites", Toast.LENGTH_SHORT).show()

            bottomSheetDialog.dismiss() // Close the bottom sheet dialog
        }

        propertiesButton.setOnClickListener {
            val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
            val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
            val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
            val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
            val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
            val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)

            // Populate dialog views with data
            fileNameTextView.text = musicList[position].title
            durationTextView.text = DateUtils.formatElapsedTime(musicList[position].duration / 1000)
            // Ensure video.size is properly converted to a numeric type
            val sizeInBytes = playlist.size.toLongOrNull() ?: 0L
            val formattedSize = Formatter.formatShortFileSize(context, sizeInBytes)
            val bytesWithCommas = NumberFormat.getInstance().format(sizeInBytes)
            sizeTextView.text = "$formattedSize ($bytesWithCommas bytes)"
            locationTextView.text = musicList[position].path

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


        playButton.setOnClickListener {
            val intent = Intent(context, PlayerMusicActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "FavouriteBottomAdapter")
            ContextCompat.startActivity(context, intent, null)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_bottom,
                R.anim.anim_no_change // Using a transparent animation for exit
            )
            bottomSheetDialog.dismiss()
        }

        shareButton.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[position].path))
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this song: ${musicList[position].title} from the album ${musicList[position].album}"
                )
            }

            ContextCompat.startActivity(
                context,
                Intent.createChooser(shareIntent, "Sharing Music File!!"),
                null
            )

            bottomSheetDialog.dismiss()
        }


        ringtoneButton.setOnClickListener {
            val builderTone = android.app.AlertDialog.Builder(context)
            val dialogViewTone =
                LayoutInflater.from(context).inflate(R.layout.favurite_ringtone, null)
            builderTone.setView(dialogViewTone)
                .setCancelable(false)

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
                        setRingtone(playlist.path)
                    }
                } else {
                    setRingtone(playlist.path)
                }
                dialogTone.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialogTone.show()
            bottomSheetDialog.dismiss()
        }

        removeButton.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.favurite_remove, null)
            builder.setView(dialogView)
                .setCancelable(false)

            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeMusicButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeMusicButton.setOnClickListener {
                // Remove the item from the list
                val removedMusic = musicList[position]
                musicList.removeAt(position)

                // Notify the adapter about the item removed
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, musicList.size)

                // Remove the item from the database
                GlobalScope.launch(Dispatchers.IO) {
                    val musicFavDao = MusicFavDatabase.getDatabase(context).musicFavDao()
                    val musicFavEntity = MusicFavEntity(
                        musicid = removedMusic.musicid,
                        title = removedMusic.title,
                        album = removedMusic.album,
                        artist = removedMusic.artist,
                        duration = removedMusic.duration,
                        path = removedMusic.path,
                        size = removedMusic.size,
                        artUri = removedMusic.artUri,
                        dateAdded = removedMusic.dateAdded
                    )
                    musicFavDao.deleteMusic(musicFavEntity)
                }

                dialog.dismiss()
                bottomSheetDialog.dismiss()
            }


            dialog.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun mapEntityToPlaylistMusic(entity: PlaylistMusicEntity): PlaylistMusic {
        return PlaylistMusic(
            musicid = entity.musicid,
            name = entity.name,
            music = listOf() // Initialize with an empty list or fetch actual music if needed
        )
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

}
