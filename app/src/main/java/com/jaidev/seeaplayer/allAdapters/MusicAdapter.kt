
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.R.*
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavEntity
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.MusicViewBinding
import com.jaidev.seeaplayer.musicActivity.NowPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.binding
import com.jaidev.seeaplayer.musicNav
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat


class MusicAdapter(
    private val context: Context,
    var musicList: ArrayList<Music>,
    private var playlistDetails: Boolean = false,
    private val isMusic: Boolean = false,
    val selectionActivity: Boolean = false

)
    : RecyclerView.Adapter<MusicAdapter.MyAdapter>() {
    private var isSelectionModeEnabled = false // Flag to track whether selection mode is active
    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomActionModeBar: View
    // Tracks selected item
    val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isAllSelected = false // Add this flag

    companion object {
        private const val PREF_NAME = "music_titles"

    }


    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Load saved music titles
        loadMusicTitles()
    }

    interface MusicDeleteListener {
        fun onMusicDeleted()
    }

    private var musicDeleteListener: MusicDeleteListener? = null

    fun setMusicDeleteListener(listener: MusicDeleteListener) {
        musicDeleteListener = listener
    }
    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionModeEnabled = false
        selectedItems.clear()
        actionMode?.finish() // Finish the ActionMode if it's active
        actionMode = null
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }


    class MyAdapter(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songName
        val album = binding.songAlbum
        val image = binding.musicViewImage
        val root = binding.root
        val more = binding.MoreChoose
        val playlstM = binding.playlistChoose2
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId", "ResourceType",
        "SuspiciousIndentation", "ObsoleteSdkInt"
    )
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        val video = musicList[position]
        holder.title.text = video.title
        holder.album.text = video.album


            Glide.with(context)
                .load(getImgArt(video.path))
                .apply(
                    RequestOptions()
                        .placeholder(color.gray) // Use the newly created drawable
                        .error(drawable.music_note_svgrepo_com) // Use the newly created drawable
                        .centerCrop()
                )
                .into(holder.image)



        // Handle selection visibility first
        if (selectedItems.contains(position)) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE
        } else {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.fillCheck.visibility = View.GONE
        }

        if (isSelectionModeEnabled) {
            holder.more.visibility = View.GONE
            if (!selectedItems.contains(position)) {
                holder.emptyCheck.visibility = View.VISIBLE
            }
        } else {
            holder.more.visibility = View.VISIBLE
            holder.emptyCheck.visibility = View.GONE
        }



        if (selectionActivity) {
            holder.more.visibility = View.GONE // Hide the more view in selection activity
            holder.playlstM.visibility = View.GONE // Hide the more view in selection activity

            holder.root.setOnClickListener {
                toggleSelection(position)
            }
        }
        else {

            holder.playlstM.visibility = View.GONE // Show the more view when not in selection activity
        }

        holder.root.setOnLongClickListener {
            toggleSelection(position)
            startActionMode()
            true
        }
        holder.root.setOnClickListener {
            if (actionMode != null) {
                toggleSelection(position)
                holder.emptyCheck.visibility = View.GONE

            } else {
                if (PlayerMusicActivity.isShuffleEnabled) {
                    PlayerMusicActivity.isShuffleEnabled = false
                    binding.shuffleBtnPA.setImageResource(drawable.shuffle_icon)
                    // If you need to perform any other actions when shuffle mode is disabled, add them here
                }
                when {
                    MainActivity.search -> sendIntent(
                        ref = "MusicAdapterSearch",
                        pos = position
                    )

                    musicList[position].id == PlayerMusicActivity.nowMusicPlayingId ->
                        sendIntent(
                            ref = "NowPlaying",
                            pos = PlayerMusicActivity.songPosition
                        )

                    else -> sendIntent(ref = "MusicAdapter", pos = position)

                }
            }



        }

        holder.more.setOnClickListener {
            val music = musicList[position]
            showBottomSheetDialog(music, position)
        }


    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "ObsoleteSdkInt", "InflateParams")
    private fun showBottomSheetDialog(video: Music , position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(layout.music_more_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        view.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

        val imageThumbnail = view.findViewById<ShapeableImageView>(R.id.imageThumbnail)
        val textTitle = view.findViewById<TextView>(R.id.textTitle)
        val textSubtitle = view.findViewById<TextView>(R.id.textSubtitle)
        val ringtoneButton = view.findViewById<LinearLayout>(R.id.ringtoneButton)
        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        val playButton = view.findViewById<LinearLayout>(R.id.playButton)
        val shareButton = view.findViewById<LinearLayout>(R.id.shareButton)
        val propertiesButton = view.findViewById<LinearLayout>(R.id.propertiesButton)
//        val addToPlaylistButton = view.findViewById<LinearLayout>(R.id.addToPlaylistButton)
        val addToFavouriteButton = view.findViewById<LinearLayout>(R.id.addToFavouriteButton)
        val renameButton = view.findViewById<LinearLayout>(R.id.renameButton)
        val favouriteIcon = view.findViewById<ImageView>(R.id.favouriteIcon)
        val favouriteTitle = view.findViewById<TextView>(R.id.favouriteTitle)


        textSubtitle.text = video.album
        textTitle.text = video.title
        Glide.with(context)
            .load(getImgArt(video.path))
            .apply(
                RequestOptions()
                    .placeholder(R.color.gray) // Use the newly created drawable
                    .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                    .centerCrop()
            )
            .into(imageThumbnail)


        // Fetch the MusicFavDao once
        val musicDao = MusicFavDatabase.getDatabase(context).musicFavDao()

        // Check if the song is already in favorites and update UI
        GlobalScope.launch(Dispatchers.Main) {
            val isFavorite = withContext(Dispatchers.IO) {
                musicDao.getAllMusic().any { it.id == video.id }
            }

            if (isFavorite) {
                favouriteIcon.setImageResource(R.drawable.round_favorite_music)
                favouriteTitle.text = "Added to Favourites"
            } else {
                favouriteIcon.setImageResource(R.drawable.round_favorite_border_music)
                favouriteTitle.text = "Add to Favourites"
            }

            addToFavouriteButton.setOnClickListener {
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


//        addToPlaylistButton.setOnClickListener {
//            // Create and show a new BottomSheetDialog for adding to playlist
//            val bottomSheetPLDialog = BottomSheetDialog(context)
//            val bottomSheetPLView = LayoutInflater.from(context).inflate(
//                R.layout.add_to_playlist_bottom_sheet, null
//            )
//            bottomSheetPLDialog.setContentView(bottomSheetPLView)
//
//            val createPlaylistButton = bottomSheetPLView.findViewById<Button>(R.id.create_playlist_button)
//            val playlistRecyclerView = bottomSheetPLView.findViewById<RecyclerView>(R.id.playlistRecyclerview)
//
//            // Set up RecyclerView
//            playlistRecyclerView.layoutManager = LinearLayoutManager(context)
//
//            // Fetch playlists from the database
//            CoroutineScope(Dispatchers.IO).launch {
//                val dao = DatabaseClientMusic.getInstance(context).playlistMusicDao()
//                val playlistEntities = dao.getAllPlaylists() // Fetch PlaylistMusicEntity list
//
//                // Convert PlaylistMusicEntity list to PlaylistMusic list
//                val playlists = playlistEntities.map { mapEntityToPlaylistMusic(it) }
//
//                withContext(Dispatchers.Main) {
//                    // Initialize and set the adapter
//                    val playlistAdapter = AddToPlaylistViewAdapter(
//                        context,
//                        playlists.toMutableList(),
//                        video, // Single song
//                        bottomSheetPLDialog,
//                        selectedSongs = emptyList() // No multiple selection here
//                    )
//                    playlistRecyclerView.adapter = playlistAdapter
//                }
//            }
//
//            // Handle the create playlist button click
//            createPlaylistButton.setOnClickListener {
//                // Inflate the new bottom sheet layout for creating a playlist
//                val createPlaylistView = LayoutInflater.from(context).inflate(
//                    R.layout.video_playlist_bottom_dialog, null
//                )
//
//                val createPlaylistDialog = BottomSheetDialog(context)
//                createPlaylistDialog.setContentView(createPlaylistView)
//
//                // Find the views in the create playlist bottom sheet layout
//                val renameField = createPlaylistView.findViewById<TextInputEditText>(R.id.renameField)
//                val createButton = createPlaylistView.findViewById<Button>(R.id.button_create_playlist)
//
//                renameField.addTextChangedListener(object : TextWatcher {
//                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//                    override fun afterTextChanged(s: Editable?) {
//                        createButton.setBackgroundColor(
//                            if (s.isNullOrEmpty()) {
//                                ContextCompat.getColor(context, R.color.button_background_default)
//                            } else {
//                                ContextCompat.getColor(context, R.color.cool_blue)
//                            }
//                        )
//                    }
//                })
//
//                createButton.setOnClickListener {
//                    val playlistName = renameField.text.toString().trim()
//                    if (playlistName.isNotEmpty()) {
//                        GlobalScope.launch(Dispatchers.IO) {
//                            val dao = DatabaseClientMusic.getInstance(context).playlistMusicDao()
//
//                            // Create a new playlist entity
//                            val newPlaylist = PlaylistMusicEntity(
//                                name = playlistName
//                            )
//
//                            // Insert the new playlist into the database and get its ID
//                            val playlistId = dao.insertPlaylist(newPlaylist)
//
//                            // Add the selected song to the newly created playlist
//                            val crossRef = PlaylistMusicCrossRef(
//                                playlistMusicId = playlistId,
//                                musicId = video.id // Assuming playlist is of type Music
//                            )
//                            dao.insertPlaylistMusicCrossRef(crossRef)
//
//                            withContext(Dispatchers.Main) {
//                                // Dismiss the dialogs
//                                createPlaylistDialog.dismiss()
//                                bottomSheetPLDialog.dismiss()
//                                val numberOfSongs = 1 // Change this if you are adding multiple songs
//                                Toast.makeText(context, "$numberOfSongs song(s) added to the playlist '$playlistName'", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    } else {
//                        // Handle empty name case (e.g., show an error message)
//                        Toast.makeText(context, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                // Show the create playlist bottom sheet
//                createPlaylistDialog.show()
//                bottomSheetPLDialog.dismiss()
//            }
//
//            bottomSheetPLDialog.show()
//            bottomSheetDialog.dismiss()
//        }

        ringtoneButton.setOnClickListener {
            val builderTone = AlertDialog.Builder(context)
            val dialogViewTone =
                LayoutInflater.from(context).inflate(layout.favurite_ringtone, null)
            builderTone.setView(dialogViewTone)
                .setCancelable(false)

            val dialogTone = builderTone.create()

            val notButton: Button = dialogViewTone.findViewById(id.not_button)
            val yesButton: Button = dialogViewTone.findViewById(id.yes_button)

            notButton.setOnClickListener {
                dialogTone.dismiss()
            }

            yesButton.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(context)) {
                        // Request permission
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:" + context.packageName)
                        startActivity(context, intent, null)
                    } else {
                        setRingtone(musicList[position].path)
                    }
                } else {
                    setRingtone(musicList[position].path)
                }
                dialogTone.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialogTone.show()
            bottomSheetDialog.dismiss()
        }
        playButton.setOnClickListener {
            sendIntent(ref = "MusicBottomPlayAdapter", pos = position)
            bottomSheetDialog.dismiss()
        }
        renameButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Get the current music title as default text
            val defaultTitle = musicList[position].title
            // Show the rename dialog with the current music title as default text
            showRenameDialog(position, defaultTitle)
        }
        deleteButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                showPermissionRequestDialog()
            } else {
                showDeleteDialog(position)
            }
        }
        propertiesButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
            val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
            val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
            val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
            val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
            val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)

            // Populate dialog views with data
            fileNameTextView.text = musicList[position].title
            durationTextView.text = DateUtils.formatElapsedTime(musicList[position].duration / 1000)
            sizeTextView.text = Formatter.formatShortFileSize(context, musicList[position].size.toLong())
            locationTextView.text = musicList[position].path

            val dialogIF = MaterialAlertDialogBuilder(context)
                .setView(customDialogIF)
                .setCancelable(false)
                .create()
            positiveButton.setOnClickListener {
                dialogIF.dismiss()
            }
            dialogIF.show()
        }
        shareButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[position].path))
            startActivity(
                context,
                Intent.createChooser(shareIntent, "Sharing Music File!!"),
                null
            )
        }


        bottomSheetDialog.show()
    }
//    private fun mapEntityToPlaylistMusic(entity: PlaylistMusicEntity): PlaylistMusic {
//        return PlaylistMusic(
//            id = entity.id,
//            name = entity.name,
//            music = listOf() // Initialize with an empty list or fetch actual music if needed
//        )
//    }

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
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionRequestDialog() {

            val dialogView = LayoutInflater.from(context).inflate(layout.video_music_delete_permission_dialog, null)
            val alertDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialogView.findViewById<Button>(id.buttonOpenSettings).setOnClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:${context.packageName}")
                startActivity(context, intent, null)
                alertDialog.dismiss()
            }

            dialogView.findViewById<Button>(id.buttonNotNow).setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()
        }


    override fun getItemCount(): Int {
        return musicList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDeleteDialog(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(layout.delete_alertdialog_music, null)


        val musicNameDelete = view.findViewById<TextView>(id.videmusicNameDelete)
        val deleteText = view.findViewById<TextView>(id.deleteText)
        val cancelText = view.findViewById<TextView>(id.cancelText)
        val iconImageView = view.findViewById<ImageView>(id.videoImage)
        // Set the delete text color to red
        deleteText.setTextColor(ContextCompat.getColor(context, color.red))

        // Set the cancel text color to black
        cancelText.setTextColor(ContextCompat.getColor(context, color.black))

        // Load video image into iconImageView using Glide
        Glide.with(context)
            .asBitmap()
            .load(getImgArt(musicList[position].path))
            .placeholder(R.color.gray) // Use the newly created drawable
            .error(R.drawable.music_note_svgrepo_com)
            .centerCrop()
            .into(iconImageView)

        musicNameDelete.text = musicList[position].title

        alertDialogBuilder.setView(view)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()
        deleteText.setOnClickListener {
            val file = File(musicList[position].path)
            if (file.exists() && file.delete()) {
                MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                MainActivity.MusicListMA.removeAt(position)
                if (musicList[position].id == PlayerMusicActivity.nowMusicPlayingId) {
                    if (PlayerMusicActivity.musicListPA.isNotEmpty()) {
                        PlayerMusicActivity.musicService?.prevNextSong(true, context)
                        musicNav.updateEmptyState()

                    } else {
                        PlayerMusicActivity.musicService?.stopService() // Stop the music service
                        PlayerMusicActivity.musicService?.mediaPlayer?.stop()
                        NowPlaying.binding.root.visibility = View.GONE
                        musicNav.updateEmptyState()

                    }
                }

                when {
                    MainActivity.search -> {
                        MainActivity.dataChanged = true
                        musicList.removeAt(position)
                        notifyDataSetChanged()
                        musicNav.updateEmptyState()

                        musicDeleteListener?.onMusicDeleted()
                    }

                    isMusic -> {
                        MainActivity.dataChanged = true
                        MainActivity.MusicListMA.removeAt(position)
                        notifyDataSetChanged()
                        musicNav.updateEmptyState()

                        musicDeleteListener?.onMusicDeleted()
                    }


                }

            } else {
                Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }

        cancelText.setOnClickListener {
            // Handle cancel action here
            alertDialog.dismiss()
        }
        alertDialog.show()
}


    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        if (selectedItems.isEmpty()) {
            actionMode?.finish()
        } else {
            startActionMode()
        }

        notifyItemChanged(position) // Update selected state for the item
        updateActionModeTitle()
        updateRenameButtonState()

        actionMode?.invalidate()
    }


    // Start action mode for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
            isSelectionModeEnabled = true // Enable selection mode
            notifyDataSetChanged() // Update all item views to hide the "more" button
            showBottomActionModeBar() // Show the custom bottom action mode bar

        }
        updateActionModeTitle()
    }
    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedItems.size} / ${musicList.size} Selected"
    }

    private fun showBottomActionModeBar() {
        // Cast context to AppCompatActivity to access layout
        val activity = context as? AppCompatActivity

        if (activity != null) {
            // Find the included layout container
            val bottomActionModeContainer = activity.findViewById<View>(id.bottom_action_mode_container)

            // Ensure the container is not null and set it to visible
            if (bottomActionModeContainer != null) {
                bottomActionModeContainer.visibility = View.VISIBLE

                // Set up click listeners
                setupBottomActionModeBarListeners()
            } else {
                Toast.makeText(context, "Bottom action mode container not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Context is not an instance of AppCompatActivity", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideBottomActionModeBar() {
        val activity = context as? AppCompatActivity

        if (activity != null) {
            // Find the included layout container
            val bottomActionModeContainer = activity.findViewById<View>(id.bottom_action_mode_container)

            // Ensure the container is not null and set it to gone
            if (bottomActionModeContainer != null) {
                bottomActionModeContainer.visibility = View.GONE
            } else {
                Toast.makeText(context, "Bottom action mode container not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Context is not an instance of AppCompatActivity", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun setupBottomActionModeBarListeners() {
        // Find buttons and set up listeners
        val activity = context as? AppCompatActivity
        val bottomActionModeContainer = activity?.findViewById<View>(id.bottom_action_mode_container)

        bottomActionModeContainer?.let {
            val checkBtn = it.findViewById<ImageView>(R.id.checkBtn)
            val shareBtn = it.findViewById<ImageView>(R.id.shareBtn)
            val infoBtn = it.findViewById<ImageView>(R.id.infoBtn)
            val renameBtn = it.findViewById<ImageView>(R.id.renameBtn)
            val deleteBtn = it.findViewById<ImageView>(R.id.deleteBtn)

            checkBtn.setOnClickListener {
                toggleSelectAllItems()
            }

            shareBtn.setOnClickListener {
                shareSelectedFiles()
            }

            infoBtn.setOnClickListener {
                if (selectedItems.size > 1) {
                    // More than one item is selected
                    val totalSize = selectedItems.sumOf { musicList[it].size.toLong() }
                    val totalSizeFormatted = Formatter.formatShortFileSize(context, totalSize)
                    val totalSizeBytesFormatted = NumberFormat.getInstance().format(totalSize)

                    // Calculate the total duration
                    val totalDuration = selectedItems.sumOf { musicList[it].duration }
                    val totalDurationFormatted = formatDurationWithApproximation(totalDuration)

                    val customDialogView = LayoutInflater.from(context).inflate(R.layout.info_dialog, null, false)
                    val containsDetailView = customDialogView.findViewById<TextView>(R.id.containsDetail)
                    val durationDetailView = customDialogView.findViewById<TextView>(R.id.durationDetail)
                    val totalSizeDetailView = customDialogView.findViewById<TextView>(R.id.totalSizeDetail)
                    val positiveButton = customDialogView.findViewById<Button>(R.id.positiveButton)

                    containsDetailView.text = "${selectedItems.size} videos"
                    durationDetailView.text = totalDurationFormatted
                    totalSizeDetailView.text = "$totalSizeFormatted ($totalSizeBytesFormatted bytes)"

                    val dialog = MaterialAlertDialogBuilder(context)
                        .setView(customDialogView)
                        .create()

                    positiveButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                }
                else if (selectedItems.size == 1) {
                    // Only one item is selected
                    val selectedInfo = SpannableStringBuilder()
                    val selectedPosition = selectedItems.first()
                    val video = musicList[selectedPosition]

                    val customDialogView = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null, false)
                    val titleView = customDialogView.findViewById<TextView>(R.id.titleText)
                    val fileNameView = customDialogView.findViewById<TextView>(R.id.fileName)
                    val durationDetailView = customDialogView.findViewById<TextView>(R.id.DurationDetail)
                    val sizeDetailView = customDialogView.findViewById<TextView>(R.id.sizeDetail)
                    val locationDetailView = customDialogView.findViewById<TextView>(R.id.locationDetail)
                    val positiveButton = customDialogView.findViewById<Button>(R.id.positiveButton)

                    titleView.text = "Properties"
                    fileNameView.text = video.title
                    durationDetailView.text = DateUtils.formatElapsedTime(video.duration / 1000)
                    sizeDetailView.text = Formatter.formatShortFileSize(context, video.size.toLong())
                    locationDetailView.text = video.path


                    val dialog = MaterialAlertDialogBuilder(context)
                        .setView(customDialogView)
                        .setCancelable(false)
                        .create()

                    positiveButton.setOnClickListener{
                        dialog.dismiss()
                    }

                    dialog.show()
                }
            }

            renameBtn.setOnClickListener {
                // Call the showRenameDialog method here
                if (selectedItems.size == 1) {
                    val selectedPosition = selectedItems.first()
                    val defaultName = musicList[selectedPosition].title
                    showRenameDialog(selectedPosition, defaultName)
                } else {
                    updateRenameButtonState()
                }
            }

            deleteBtn.setOnClickListener {
                if (selectedItems.isNotEmpty()) {
                    // Build confirmation dialog
                    val message = if (playlistDetails) {
                        "Are you sure you want to delete these ${selectedItems.size} selected musics? This will permanently delete them."
                    } else {
                        "Are you sure you want to delete these ${selectedItems.size} selected musics?"
                    }

                    AlertDialog.Builder(context)
                        .setTitle("Confirm Delete")
                        .setMessage(message)
                        .setPositiveButton("Delete") { _, _ ->
                            // User clicked Delete, proceed with deletion
                            val positionsToDelete = ArrayList(selectedItems)
                            positionsToDelete.sortDescending()

                            for (position in positionsToDelete) {
                                val music = musicList[position]
                                val file = File(music.path)

                                if (file.exists() && file.delete()) {
                                    MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                    notifyItemChanged(position)

                                    if (musicList[position].id == PlayerMusicActivity.nowMusicPlayingId) {
                                        if (PlayerMusicActivity.musicListPA.isNotEmpty()) {
                                            musicNav.updateEmptyState()
                                            PlayerMusicActivity.musicService?.prevNextSong(true, context)
                                        } else {
                                            PlayerMusicActivity.musicService?.stopService() // Stop the music service
                                            PlayerMusicActivity.musicService?.mediaPlayer?.stop()
                                            NowPlaying.binding.root.visibility = View.GONE
                                            musicNav.updateEmptyState()

                                        }
                                    }
                                    musicList.removeAt(position)
                                    musicNav.updateEmptyState()

                                }
                            }
                            musicNav.updateEmptyState()
                            selectedItems.clear()
                            notifyDataSetChanged()
                            musicDeleteListener?.onMusicDeleted()
                            updateActionModeTitle()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

            }

        }

    }
    private fun updateRenameButtonState() {
        val activity = context as? AppCompatActivity
        val bottomActionModeContainer = activity?.findViewById<View>(id.bottom_action_mode_container)
        bottomActionModeContainer?.let {
            val renameBtn = it.findViewById<ImageView>(R.id.renameBtn)
            if (selectedItems.size != 1) {
                renameBtn.isEnabled = false
                renameBtn.setColorFilter(ContextCompat.getColor(context, color.gray), PorterDuff.Mode.SRC_IN)
            } else {
                renameBtn.isEnabled = true
                renameBtn.clearColorFilter()
            }
        }
    }
    private fun formatDurationWithApproximation(duration: Long): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d (%d hours approx)", hours, minutes % 60, seconds % 60, hours)
            minutes > 0 -> String.format("%02d:%02d (%d minutes approx)", minutes, seconds % 60, minutes)
            else -> String.format("%02d (%d seconds)", seconds, seconds)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelectAllItems() {
        isAllSelected = if (isAllSelected) {
            // Unselect all items
            selectedItems.clear()
            false
        } else {
            // Select all items
            for (i in 0 until musicList.size) {
                selectedItems.add(i)
            }
            true
        }
        notifyDataSetChanged()
        updateActionModeTitle()


    }
    // Action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {


            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

            when (item?.itemId) {
                id.playMulti -> {


                }

                id.infoMulti -> {

                }

            }
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            isSelectionModeEnabled = false // Enable selection mode
            hideBottomActionModeBar() // Hide the custom bottom action mode bar

            notifyDataSetChanged()
        }
    }


    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()
        for (position in selectedItems) {
            val music = musicList[position]
            val file = File(music.path)
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
            uris.add(fileUri)
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "audio/*"
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val packageManager = context.packageManager
        val resolvedActivityList = packageManager.queryIntentActivities(shareIntent, 0)
        val excludedComponents = mutableListOf<ComponentName>()

        for (resolvedActivity in resolvedActivityList) {
            if (resolvedActivity.activityInfo.packageName == context.packageName) {
                excludedComponents.add(
                    ComponentName(
                        resolvedActivity.activityInfo.packageName,
                        resolvedActivity.activityInfo.name
                    )
                )
            }
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Files")
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooserIntent)

    }

    private fun renameMusic(position: Int, newName: String) {
        val music = musicList[position]
        music.title = newName
        notifyItemChanged(position)
        saveMusicTitle(music.id, newName)
        val defaultTitle = music.title
        showRenameDialog(position, defaultTitle)


    }


    private fun showRenameDialog(position: Int, defaultTitle: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(layout.rename_field, null)
        val editText = view.findViewById<EditText>(id.renameField)
        editText.setText(defaultTitle) // Set default text as current music title

        dialogBuilder.setView(view)
            .setTitle("Rename Music")
            .setMessage("Enter new name for the music:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameMusic(position, newName)
                    // Dismiss the dialog after performing the rename action
                    dialogRF.dismiss()
                } else {
                    Toast.makeText(context, "Name can't be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { self, _ ->
                self.dismiss()
            }
        dialogRF = dialogBuilder.create()
        dialogRF.show()
        // Set the positive and negative button colors to cool_blue
        dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
        dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.cool_blue))

    }


    private fun saveMusicTitle(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }


    private fun loadMusicTitles() {
        for (music in musicList) {
            val savedTitle = sharedPreferences.getString(music.id, null)
            savedTitle?.let {
                music.title = it
            }
        }
    }



    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, PlayerMusicActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        startActivity(context, intent, null)
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateMusicList(searchList: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        this.musicList = searchList
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun addAll(selectedSongs: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(selectedSongs)
        notifyDataSetChanged()
    }


}
