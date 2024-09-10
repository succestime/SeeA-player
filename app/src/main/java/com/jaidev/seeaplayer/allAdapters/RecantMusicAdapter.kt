package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MusicFavouriteData.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavouriteData.MusicFavEntity
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.setReSongPosition
import com.jaidev.seeaplayer.databinding.RecantMusicViewBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.recantFragment.DaysMusic
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity.Companion.binding
import com.jaidev.seeaplayer.recantFragment.ReNowPlaying
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat

class RecantMusicAdapter (val  context : Context,
                          var musicReList : ArrayList<RecantMusic>,
                          val isReMusic: Boolean = false,
                        val fileCountChangeListener: OnFileCountChangeListener ,
                          private val isMusic: Boolean = false,



                          ): RecyclerView.Adapter<RecantMusicAdapter.MyAdapter>() {

    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isSelectionModeEnabled = false // Flag to track whether selection mode is active
    private var isAllSelected = false // Add this flag


    private var musicDeleteListener: MusicDeleteListener? = null

    fun setMusicDeleteListener(listener: MusicDeleteListener) {
        musicDeleteListener = listener
    }
    interface MusicDeleteListener {
        fun onMusicDeleted()
    }


    interface OnFileCountChangeListener {
        fun onFileCountChanged(newCount: Int)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionModeEnabled = false
        selectedItems.clear()
        actionMode?.finish() // Finish the ActionMode if it's active
        actionMode = null
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }


    class MyAdapter(binding: RecantMusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        var title = binding.songName
        val image = binding.musicViewImage
        val album = binding.songAlbum
        val root = binding.root
        val more = binding.MoreChoose
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        return MyAdapter(
            RecantMusicViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        val video = musicReList[position]

        holder.title.text = video.title
        holder.album.text = video.album
        Glide.with(context)
            .load(getImgArt(video.path))
            .apply(RequestOptions()
                .placeholder(R.color.gray) // Use the newly created drawable
                .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                .centerCrop())
            .into(holder.image)



        if (selectedItems.contains(position)) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE
        } else {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.fillCheck.visibility = View.GONE
        }

        // Hide or show the more button based on selection mode
        // Adjust for selection mode
        if (isSelectionModeEnabled) {
            holder.more.visibility = View.GONE
            if (!selectedItems.contains(position)) {
                holder.emptyCheck.visibility = View.VISIBLE
            }
        } else {
            holder.more.visibility = View.VISIBLE
            holder.emptyCheck.visibility = View.GONE
        }

        holder.root.setOnLongClickListener {
            toggleSelection(position)
            startActionMode()
            true
        }
        holder.root.setOnClickListener {
            if (actionMode != null) {
                toggleSelection(position)
            } else {
                if (ReMusicPlayerActivity.isShuffleEnabled) {
                    ReMusicPlayerActivity.isShuffleEnabled = false
                    binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
                }
                when {
                    isReMusic -> {
                        val intent = Intent(context, ReMusicPlayerActivity::class.java)
                        intent.putExtra("index", position)
                        intent.putExtra("class", "RecantMusicAdapter")
                        ContextCompat.startActivity(context, intent, null)
                        (context as Activity).overridePendingTransition(
                            R.anim.slide_in_bottom,
                            R.anim.anim_no_change // Using a transparent animation for exit
                        )
                    }
                }
            }
        }

        holder.more.setOnClickListener {
            showBottomSheetDialog(video, position)

        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "ObsoleteSdkInt", "InflateParams")
    private fun showBottomSheetDialog(video: RecantMusic, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.re_music_more_bottom_sheet, null)
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
        val addToFavouriteButton = view.findViewById<LinearLayout>(R.id.addToFavouriteButton)
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
                musicDao.getAllMusic().any { it.musicid == video.id }
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
                                musicid = video.id,
                                title = video.title,
                                album = video.album,
                                artist = video.artist,
                                duration = video.duration,
                                path = video.path,
                                size = video.size,
                                artUri = video.albumArtUri.toString(),
                                dateAdded = video.timestamp
                            )
                        )
                    } else {
                        musicDao.insertMusic(
                            MusicFavEntity(
                                musicid = video.id,
                                title = video.title,
                                album = video.album,
                                artist = video.artist,
                                duration = video.duration,
                                path = video.path,
                                size = video.size,
                                artUri = video.albumArtUri.toString(),
                                dateAdded = video.timestamp
                            )
                        )
                    }
                }
                bottomSheetDialog.dismiss()
            }
        }


        ringtoneButton.setOnClickListener {
            val builderTone = AlertDialog.Builder(context)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(context)) {
                        // Request permission
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
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

        playButton.setOnClickListener {
            sendIntent(ref = "MusicBottomPlayAdapter", pos = position)
            bottomSheetDialog.dismiss()
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
            fileNameTextView.text = musicReList[position].title
            durationTextView.text = DateUtils.formatElapsedTime(musicReList[position].duration / 1000)
            // Ensure video.size is properly converted to a numeric type
            val sizeInBytes = video.size.toLongOrNull() ?: 0L
            val formattedSize = Formatter.formatShortFileSize(context, sizeInBytes)
            val bytesWithCommas = NumberFormat.getInstance().format(sizeInBytes)
            sizeTextView.text = "$formattedSize ($bytesWithCommas bytes)"

            locationTextView.text = musicReList[position].path

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
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicReList[position].path))
            ContextCompat.startActivity(
                context,
                Intent.createChooser(shareIntent, "Sharing Music File!!"),
                null
            )

        }


        bottomSheetDialog.show()
    }

    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, ReMusicPlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
        (context as Activity).overridePendingTransition(
            R.anim.slide_in_bottom,
            R.anim.anim_no_change // Using a transparent animation for exit
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

    override fun getItemCount(): Int {
        return musicReList.size
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
        actionMode?.invalidate()

    }

    // Start action mode for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
            isSelectionModeEnabled = true // Enable selection mode
            notifyDataSetChanged() // Update all item views to hide the "more" button
        }
        updateActionModeTitle()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedItems.size} / ${musicReList.size} Selected"
    }
    // Action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate action mode menu
            mode?.menuInflater?.inflate(R.menu.multiple_re_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Hide the menu_rename item if more than one item is selected
            val renameItem = menu?.findItem(R.id.renameMulti)
            renameItem?.isVisible = selectedItems.size == 1

            return true
        }


        @SuppressLint("NotifyDataSetChanged", "ResourceType")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.shareMulti -> {
                    shareSelectedFiles()
                }
                R.id.deleteMulti -> {
                    if (selectedItems.isNotEmpty()) {
                        // Build confirmation dialog
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete these ${selectedItems.size} selected videos?")
                            .setPositiveButton("Delete") { _, _ ->
                                // User clicked Delete, proceed with deletion
                                val positionsToDelete = ArrayList(selectedItems)
                                positionsToDelete.sortDescending()

                                var shouldBreak = false

                                for (position in positionsToDelete) {
                                    if (shouldBreak) break

                                    val video = musicReList[position]
                                    val file = File(video.path)

                                    if (file.exists() && file.delete()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                        PlayerMusicActivity.musicService?.stopService()

                                        val currentlyPlayingMusic = ReMusicPlayerActivity.reMusicListPA.getOrNull(ReMusicPlayerActivity.songPosition)
                                        val isCurrentlyPlaying = currentlyPlayingMusic?.path == video.path

                                        // Check if there is only one last music in the list and it is not currently playing
                                        if (musicReList.size == 1 && !isCurrentlyPlaying) {
                                            // Remove the last music item from the list
                                            musicReList.removeAt(position)
                                            notifyItemRemoved(position)
                                            DaysMusic.updateEmptyViewVisibility()
                                            notifyDataSetChanged()
                                            actionMode?.finish()
                                            fileCountChangeListener.onFileCountChanged(musicReList.size)
                                            musicDeleteListener?.onMusicDeleted()
                                            shouldBreak = true
                                            break
                                        }

                                        // Check if there are only a few songs left and the user is deleting the currently playing song
                                        if (musicReList.size <= 3 && isCurrentlyPlaying && musicReList.size - 1 == position) {
                                            // No more songs to play, stop the music service
                                            ReMusicPlayerActivity.musicService!!.mediaPlayer!!.stop()
                                            ReNowPlaying.binding.root.visibility = View.GONE
                                            DaysMusic.updateEmptyViewVisibility()
                                            musicReList.removeAt(position)
                                            notifyItemRemoved(position)
                                            notifyDataSetChanged()
                                            actionMode?.finish()
                                            fileCountChangeListener.onFileCountChanged(musicReList.size)
                                            musicDeleteListener?.onMusicDeleted()
                                            shouldBreak = true
                                            break
                                        }

                                        if (isCurrentlyPlaying) {
                                            // Stop the music service
                                            PlayerMusicActivity.musicService?.stopService()

                                            // Remove the deleted song from the list
                                            ReMusicPlayerActivity.reMusicListPA.removeAt(ReMusicPlayerActivity.songPosition)

                                            // Check if there are other songs to play
                                            if (ReMusicPlayerActivity.reMusicListPA.isNotEmpty()) {
                                                // Adjust the song position if necessary
                                                if (ReMusicPlayerActivity.songPosition >= ReMusicPlayerActivity.reMusicListPA.size) {
                                                    ReMusicPlayerActivity.songPosition = 0
                                                }

                                                // Move to the next song and update the player
                                                setReSongPosition(increment = true)
                                                ReMusicPlayerActivity.createMediaPlayer(context)
                                                ReMusicPlayerActivity.setLayout(context)
                                                DaysMusic.updateEmptyViewVisibility()
                                                Glide.with(context)
                                                    .asBitmap()
                                                    .load(getImgArt(ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].path))
                                                    .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                                                    .into(ReNowPlaying.binding.songImgNP)
                                                ReNowPlaying.binding.songNameNP.text = ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].title
                                            } else {
                                                DaysMusic.updateEmptyViewVisibility()
                                                ReMusicPlayerActivity.musicService!!.mediaPlayer!!.stop()
                                                ReNowPlaying.binding.root.visibility = View.GONE
                                            }
                                            DaysMusic.updateEmptyViewVisibility()
                                        }
                                        // Always remove the music item from the list, regardless of whether it was playing
                                        musicReList.removeAt(position)
                                    }
                                }
                                musicDeleteListener?.onMusicDeleted()

                                selectedItems.clear()
                                notifyDataSetChanged()
                                updateActionModeTitle()
                                DaysMusic.updateEmptyViewVisibility()
                                fileCountChangeListener.onFileCountChanged(musicReList.size)
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                // User clicked Cancel, dismiss dialog
                                dialog.dismiss()
                            }
                            .show()
                        updateActionModeTitle()
                    }
                    return true
                }

                R.id.checkMulti -> {
                    toggleSelectAllItems(item)

                }
            }
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            isSelectionModeEnabled = false // Disable selection mode
            notifyDataSetChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun toggleSelectAllItems(item: MenuItem) {

            isAllSelected = if (isAllSelected) {
                // Unselect all items
                selectedItems.clear()
                item.setIcon(R.drawable.round_crop_square_24)
                false
            } else {
                // Select all items
                for (i in 0 until musicReList.size) {
                    selectedItems.add(i)
                }
                item.setIcon(R.drawable.check_box_24)
                true
            }
            updateActionModeTitle()
            notifyDataSetChanged()
        }
    }
    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()
        for (position in selectedItems) {
            val music = musicReList[position]
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


    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId")
    private fun showDeleteDialog(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.delete_alertdialog_music, null)


        val musicNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        val cancelText = view.findViewById<TextView>(R.id.cancelText)
        val iconImageView = view.findViewById<ImageView>(R.id.videoImage)
        // Set the delete text color to red
        deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

        // Set the cancel text color to black
        cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Load video image into iconImageView using Glide
        Glide.with(context)
            .asBitmap()
            .load(getImgArt(musicReList[position].path))
            .placeholder(R.color.gray) // Use the newly created drawable
            .error(R.drawable.music_note_svgrepo_com)
            .centerCrop()
            .into(iconImageView)

        musicNameDelete.text = musicReList[position].title

        alertDialogBuilder.setView(view)

        val alertDialog = alertDialogBuilder.create()
        deleteText.setOnClickListener {
            deleteVideo(position)

            alertDialog.dismiss()

        }

        cancelText.setOnClickListener {
            // Handle cancel action here
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionRequestDialog() {

        val dialogView = LayoutInflater.from(context).inflate(R.layout.video_music_delete_permission_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
            ContextCompat.startActivity(context, intent, null)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonNotNow).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }



    @SuppressLint("NotifyDataSetChanged")
    private fun deleteVideo(position: Int) {
        val music = musicReList[position]
        val file = File(music.path)

        if (file.exists() && file.delete()) {
            MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
            PlayerMusicActivity.musicService?.stopService() // Stop the music service

            val currentlyPlayingMusic = ReMusicPlayerActivity.reMusicListPA.getOrNull(ReMusicPlayerActivity.songPosition)
            val isCurrentlyPlaying = currentlyPlayingMusic?.path == music.path

            // Check if there is only one last music in the list and it is not currently playing
            if (musicReList.size == 1 && !isCurrentlyPlaying) {
                musicReList.removeAt(position)
                notifyItemRemoved(position)
                notifyDataSetChanged()
                actionMode?.finish()
                DaysMusic.updateEmptyViewVisibility()

                fileCountChangeListener.onFileCountChanged(musicReList.size)
                musicDeleteListener?.onMusicDeleted()
                return
            }

            // Check if there are only a few songs left and the user is deleting the currently playing song
            if (musicReList.size <= 3 && isCurrentlyPlaying && musicReList.size - 1 == position) {
                // No more songs to play, stop the music service
                ReMusicPlayerActivity.musicService!!.mediaPlayer!!.stop()
                ReNowPlaying.binding.root.visibility = View.GONE
                musicReList.removeAt(position)
                notifyItemRemoved(position)
                notifyDataSetChanged()
                actionMode?.finish()
                DaysMusic.updateEmptyViewVisibility()

                fileCountChangeListener.onFileCountChanged(musicReList.size)
                musicDeleteListener?.onMusicDeleted()

                return
            }

            if (isCurrentlyPlaying) {
                // Stop the music service
                PlayerMusicActivity.musicService?.stopService()



                // Check if there are other songs to play
                if (ReMusicPlayerActivity.reMusicListPA.isNotEmpty()) {
                    // Adjust the song position if necessary
                    if (ReMusicPlayerActivity.songPosition >= ReMusicPlayerActivity.reMusicListPA.size) {
                        ReMusicPlayerActivity.songPosition = 0
                    }

                    // Move to the next song and update the player
                    setReSongPosition(increment = true)
                    ReMusicPlayerActivity.createMediaPlayer(context)
                    ReMusicPlayerActivity.setLayout(context)
                    DaysMusic.updateEmptyViewVisibility()

                    Glide.with(context)
                        .asBitmap()
                        .load(getImgArt(ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].path))
                        .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                        .into(ReNowPlaying.binding.songImgNP)
                    ReNowPlaying.binding.songNameNP.text = ReMusicPlayerActivity.reMusicListPA[ReMusicPlayerActivity.songPosition].title
                } else {
                    DaysMusic.updateEmptyViewVisibility()
                    ReMusicPlayerActivity.musicService!!.mediaPlayer!!.stop()
                    ReNowPlaying.binding.root.visibility = View.GONE
                }
                DaysMusic.updateEmptyViewVisibility()

            }

            // Always remove the music item from the list, regardless of whether it was playing
            musicReList.removeAt(position)
            notifyItemRemoved(position)
            notifyDataSetChanged()
            actionMode?.finish()
            fileCountChangeListener.onFileCountChanged(musicReList.size)
            musicDeleteListener?.onMusicDeleted()
            DaysMusic.updateEmptyViewVisibility()
        } else {
            Toast.makeText(context, "Failed to delete music", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateRecentMusics(recantMusic: List<RecantMusic>) {
        musicReList.clear()
        musicReList.addAll(recantMusic)
        notifyDataSetChanged()
    }




}

