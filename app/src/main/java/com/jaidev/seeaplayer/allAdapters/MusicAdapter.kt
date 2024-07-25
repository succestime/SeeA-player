
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.R.*
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.MusicViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import com.jaidev.seeaplayer.musicActivity.NowPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.binding
import com.jaidev.seeaplayer.musicActivity.PlaylistActivity
import com.jaidev.seeaplayer.musicActivity.PlaylistDetails
import java.io.File


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

    // Tracks selected item
    val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null

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

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId", "ResourceType",
        "SuspiciousIndentation"
    )
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {

        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album


            Glide.with(context)
                .load(getImgArt(musicList[position].path))
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

        when {
            playlistDetails -> {

                holder.root.setOnClickListener {
                    if (actionMode != null) {
                        // If action mode is active, toggle selection as usual
                        toggleSelection(position)
                    }else {
                        sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                    }
                }

                holder.root.setOnLongClickListener {
                    toggleSelection(position)
                    startActionMode()
                    true
                }

                // Adjust for selection mode
                if (isSelectionModeEnabled) {
                    holder.playlstM.visibility = View.GONE
                    if (!selectedItems.contains(position)) {
                        holder.emptyCheck.visibility = View.VISIBLE
                    }
                } else {
                    holder.playlstM.visibility = View.VISIBLE
                    holder.emptyCheck.visibility = View.GONE
                }

                holder.more.visibility = View.GONE

                holder.playlstM.setOnClickListener { view ->
                    val popupMenu = PopupMenu(context, view)
                    popupMenu.inflate(menu.playlist_remove)
                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            id.remove_item -> {
                                // Handle the removal of the item from the playlist
                                removeItemFromPlaylist(position)
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            }

            selectionActivity ->{
         holder.emptyCheck.visibility = View.VISIBLE
                holder.root.setOnClickListener {
                    if(addSong(musicList[position])) {
                        holder.emptyCheck.visibility = View.GONE
                        holder.fillCheck.visibility = View.VISIBLE
                    }
                    else{
                    holder.emptyCheck.visibility = View.VISIBLE
                    holder.fillCheck.visibility = View.GONE
                }
                }
            }
        }
        // Show/hide multi-select icon based on selection
//        holder.button.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE

        holder.more.setOnClickListener {
            newPosition = position
            // Inflate the custom dialog layout
            val customDialog = LayoutInflater.from(context).inflate(layout.video_more_features, holder.root, false)
            val bindingMf = VideoMoreFeaturesBinding.bind(customDialog)
            // Create the dialog
            val dialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(customDialog)
            val dialog = dialogBuilder.create()
            dialog.show()

            // Get the window attributes of the dialog
            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set dialog width and height
            window?.setGravity(Gravity.BOTTOM) // Set dialog gravity to bottom

            bindingMf.shareBtn.setOnClickListener {

                dialog.dismiss()
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
            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIf = LayoutInflater.from(context)
                    .inflate(layout.details_view, holder.root, false)
                val bindingIf = DetailsViewBinding.bind(customDialogIf)
                val dialogIf = MaterialAlertDialogBuilder(context).setView(customDialogIf)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->


                        self.dismiss()
                    }
                    .create()
                dialogIf.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(musicList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(musicList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            musicList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(musicList[position].path)
                bindingIf.detailTV.text = infoText
            }

            bindingMf.renameBtn.setOnClickListener {
                dialog.dismiss()
                // Get the current music title as default text
                val defaultTitle = musicList[position].title
                // Show the rename dialog with the current music title as default text
                showRenameDialog(position, defaultTitle)
            }

            bindingMf.deleteBtn.setOnClickListener {
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    showPermissionRequestDialog()
                } else {
                    showDeleteDialog(position)
                }

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
                ContextCompat.startActivity(context, intent, null)
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
            .apply(RequestOptions().placeholder(mipmap.ic_logo_o).centerCrop())
            .into(iconImageView)

        musicNameDelete.text = musicList[position].title

        alertDialogBuilder.setView(view)

        val alertDialog = alertDialogBuilder.create()
        deleteText.setOnClickListener {
            val file = File(musicList[position].path)
            if (file.exists() && file.delete()) {
                MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                MainActivity.MusicListMA.removeAt(position)
                if (musicList[position].id == PlayerMusicActivity.nowMusicPlayingId) {
                    if (PlayerMusicActivity.musicListPA.isNotEmpty()) {
                        PlayerMusicActivity.musicService?.prevNextSong(true, context)
                    } else {
                        PlayerMusicActivity.musicService?.stopService() // Stop the music service
                        PlayerMusicActivity.musicService?.mediaPlayer?.stop()
                        NowPlaying.binding.root.visibility = View.GONE
                    }
                }

                when {
                    MainActivity.search -> {
                        MainActivity.dataChanged = true
                        musicList.removeAt(position)
                        notifyDataSetChanged()
                        musicDeleteListener?.onMusicDeleted()
                    }

                    isMusic -> {
                        MainActivity.dataChanged = true
                        MainActivity.MusicListMA.removeAt(position)
                        notifyDataSetChanged()
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

    private fun removeItemFromPlaylist(position: Int) {
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(position)
        notifyItemRangeChanged(position, musicList.size)
    }


    private fun addSong(song: Music): Boolean{
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if(song.id == music.id){
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }

    // Toggle selection for multi-select
    // Toggle selection for multi-select
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
            mode?.menuInflater?.inflate(R.menu.multiple_player_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Hide the menu_rename item if more than one item is selected
            val renameItem = menu?.findItem(id.renameMulti)
            renameItem?.isVisible = selectedItems.size == 1

            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            // Handle action mode menu items
            val actionMode = mode
            when (item?.itemId) {
                id.renameMulti -> {
                    // Call the showRenameDialog method here
                    if (selectedItems.size == 1) {
                        val selectedPosition = selectedItems.first()
                        val defaultName = musicList[selectedPosition].title
                        showRenameDialog(selectedPosition, defaultName)
                    } else {
                        Toast.makeText(
                            context,
                            "Please select only one music to rename",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return true
                }

                id.shareMulti -> {
                    shareSelectedFiles()
                }
                id.deleteMulti -> {
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
                                                // Notify the service to play the next song
                                                PlayerMusicActivity.musicService?.prevNextSong(true, context)
                                            } else {
                                                PlayerMusicActivity.musicService?.stopService() // Stop the music service
                                                PlayerMusicActivity.musicService?.mediaPlayer?.stop()
                                                NowPlaying.binding.root.visibility = View.GONE
                                            }
                                        }
                                        musicList.removeAt(position)
                                    }
                                }

                                selectedItems.clear()
                                mode?.finish()
                                notifyDataSetChanged()
                                musicDeleteListener?.onMusicDeleted()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    return true
                }
                // Add more action mode items as needed
            }
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            isSelectionModeEnabled = false // Enable selection mode
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

        actionMode?.finish()
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
        // Dismiss action mode
        actionMode?.finish()
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
    fun refreshPlaylist() {
        musicList = ArrayList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addAll(selectedSongs: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(selectedSongs)
        notifyDataSetChanged()
    }


}



