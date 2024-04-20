package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RecantMusicViewBinding
import com.jaidev.seeaplayer.databinding.RecantVideoMoreFeaturesBinding
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity
import java.io.File

class RecantMusicAdapter (val  context : Context,  var musicReList : ArrayList<RecantMusic>, val isReMusic: Boolean = false,
                         ): RecyclerView.Adapter<RecantMusicAdapter.MyAdapter>() {

    private var newPosition = 0
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null


    class MyAdapter(binding: RecantMusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        var title = binding.songName
        val image = binding.musicViewImage
        val album = binding.songAlbum
        val root = binding.root
        val more = binding.MoreChoose
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
            .asBitmap()
            .load(musicReList[position].albumArtUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three)).centerCrop()
            .into(holder.image)


        // Determine if the item is currently selected
        if (selectedItems.contains(position)) {
            // Set your custom selected background on the root view of the item
            holder.root.setBackgroundResource(R.drawable.browser_selected_background)
        } else {
            // Reset to default background based on app theme
            holder.root.setBackgroundResource(android.R.color.transparent)

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

                val selectedPosition = newPosition  // Use newPosition or another variable to identify the selected video
                showSingleDeleteConfirmation(selectedPosition)
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
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
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
//                R.id.renameMulti -> {
////                    // Call the showRenameDialog method here
////                    if (selectedItems.size == 1) {
////                        val selectedPosition = selectedItems.first()
////                        val defaultName = videoReList[selectedPosition].title
//////                        showRenameDialog(selectedPosition, defaultName)
////                    } else {
////                        Toast.makeText(context, "Please select only one video to rename", Toast.LENGTH_SHORT).show()
////                    }
////                    return true
//                }

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
                                        musicReList.removeAt(position)
                                    }
                                }

                                selectedItems.clear()
                                mode?.finish()
                                notifyDataSetChanged()
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
            notifyDataSetChanged()
        }
    }

    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()
        // Iterate through selectedItems to get selected file items
        for (position in selectedItems) {
            val music = musicReList[position]
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(music.path)
            )
            uris.add(fileUri)
        }

        // Create an ACTION_SEND intent to share multiple files
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "*/*"
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Start the intent chooser to share multiple files
        val chooser = Intent.createChooser(shareIntent, "Share Files")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
    private fun showSingleDeleteConfirmation(position: Int) {
        val video = musicReList[position]

        AlertDialog.Builder(context)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete '${video.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVideo(position)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    // Function to delete a single video
    private fun deleteVideo(position: Int) {
        val music = musicReList[position]
        val file = File(music.path)

        if (file.exists() && file.delete()) {
            MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
            musicReList.removeAt(position)
            notifyItemRemoved(position)
            Toast.makeText(context, "Music deleted successfully", Toast.LENGTH_SHORT).show()

            // If action mode is active, finish it after deletion
            actionMode?.finish()
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
//    private fun sendIntent(pos: Int, ref: String) {
//        ReMusicPlayerActivity.position = pos
//        val intent = Intent(context, ReMusicPlayerActivity::class.java)
//        intent.putExtra("class", ref)
//        ContextCompat.startActivity(context, intent, null)
//
//    }
}