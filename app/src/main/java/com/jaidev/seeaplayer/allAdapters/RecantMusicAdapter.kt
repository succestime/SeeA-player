package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.reSetSongPosition
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RecantMusicViewBinding
import com.jaidev.seeaplayer.databinding.RecantVideoMoreFeaturesBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity.Companion.binding
import com.jaidev.seeaplayer.recantFragment.ReNowPlaying
import java.io.File

class RecantMusicAdapter (val  context : Context,
                          var musicReList : ArrayList<RecantMusic>,
                          val isReMusic: Boolean = false,
                        val fileCountChangeListener: OnFileCountChangeListener ,
                          private val isMusic: Boolean = false,



                          ): RecyclerView.Adapter<RecantMusicAdapter.MyAdapter>() {

    private var newPosition = 0
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isSelectionModeEnabled = false // Flag to track whether selection mode is active


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
        holder.title.text = musicReList[position].title
        holder.album.text = musicReList[position].album
        Glide.with(context)
            .load(getImgArt(musicReList[position].path))
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
                // If action mode is active, toggle selection as usual
                toggleSelection(position)
            } else {
                if (ReMusicPlayerActivity.isShuffleEnabled) {
                    ReMusicPlayerActivity.isShuffleEnabled = false
                    binding.shuffleBtnPA.setImageResource(R.drawable.shuffle_icon)
                    // If you need to perform any other actions when shuffle mode is disabled, add them here
                }
                when {
                    isReMusic -> {
                        val intent = Intent(context, ReMusicPlayerActivity::class.java)
                        intent.putExtra("index", position)
                        intent.putExtra("class", "RecantMusicAdapter")
                        ContextCompat.startActivity(context, intent, null)
                    }
                }
            }
        }

        holder.more.setOnClickListener {
            newPosition = position
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.recant_video_more_features, holder.root, false)
            val bindingMf = RecantVideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()
            // Get the window attributes of the dialog
            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set dialog width and height
            window?.setGravity(Gravity.BOTTOM) // Set dialog gravity to bottom


            bindingMf.deleteBtn.setOnClickListener {
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    showPermissionRequestDialog()
                } else {
                    showDeleteDialog(position)
                }
            }
            bindingMf.shareBtn.setOnClickListener {

                dialog.dismiss()
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
            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIf = LayoutInflater.from(context)
                    .inflate(R.layout.details_view, holder.root, false)
                val bindingIf = DetailsViewBinding.bind(customDialogIf)
                val dialogIf = MaterialAlertDialogBuilder(context).setView(customDialogIf)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->


                        self.dismiss()
                    }
                    .create()
                dialogIf.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(musicReList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(musicReList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            musicReList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(musicReList[position].path)
                bindingIf.detailTV.text = infoText
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
        actionMode?.title = "${selectedItems.size} selected" // Update action mode title
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
        actionMode?.title = "${selectedItems.size} selected"
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

                                for (position in positionsToDelete) {
                                    val video = musicReList[position]
                                    val file = File(video.path)

                                    if (file.exists() && file.delete()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                        PlayerMusicActivity.musicService?.stopService() // Stop the music service

                                        musicReList.removeAt(position)
                                    }
                                }
                                musicDeleteListener?.onMusicDeleted()

                                selectedItems.clear()
                                mode?.finish()
                                notifyDataSetChanged()
                                // Dismiss action mode
                                actionMode?.finish()
                                // Notify listener of the file count change
                                fileCountChangeListener.onFileCountChanged(musicReList.size)
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                // User clicked Cancel, dismiss dialog
                                dialog.dismiss()
                            }
                            .show()
                    }
                    return true
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

        actionMode?.finish()
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
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
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
            // Check if the deleted music is the currently playing one
            if (ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path == music.path) {
                // Move to the next song
                reSetSongPosition(increment = true)
                ReMusicPlayerActivity.createMediaPlayer(context)
                ReMusicPlayerActivity.setLayout(context)

                Glide.with(context)
                    .asBitmap()
                    .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
                    .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                    .into(ReNowPlaying.binding.songImgNP)
                ReNowPlaying.binding.songNameNP.text = ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].title
            }

            when {
                isMusic -> {
                    MainActivity.dataChanged = true
                    MainActivity.MusicListMA.removeAt(position)
                    notifyDataSetChanged()
                    musicDeleteListener?.onMusicDeleted()
                }
            }

            musicReList.removeAt(position)
            notifyItemRemoved(position)
            notifyDataSetChanged()
            actionMode?.finish()
            fileCountChangeListener.onFileCountChanged(musicReList.size)
            musicDeleteListener?.onMusicDeleted()
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