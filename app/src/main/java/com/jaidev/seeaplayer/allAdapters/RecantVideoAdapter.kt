
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
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
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.RecantDownloadViewBinding
import com.jaidev.seeaplayer.databinding.RecantVideoMoreFeaturesBinding
import com.jaidev.seeaplayer.recantFragment.ReVideoPlayerActivity
import java.io.File

class RecentVideoAdapter(private val context: Context,
                         private var videoReList: ArrayList<RecantVideo> ,
                         private val isRecantVideo: Boolean = false,
                         val fileCountChangeListener: OnFileCountChangeListener
) :
    RecyclerView.Adapter<RecentVideoAdapter.MyAdapter>() {
    interface OnFileCountChangeListener {
        fun onFileCountChanged(newCount: Int)
    }
    private  var newPosition = 0
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isSelectionModeEnabled = false // Flag to track whether selection mode is active

    class MyAdapter(binding: RecantDownloadViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val more = binding.MoreChoose
        val root = binding.root
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter {
        val binding = RecantDownloadViewBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return MyAdapter(binding)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: MyAdapter, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = videoReList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoReList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoReList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.image)

        if (selectedItems.contains(position)) {
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.VISIBLE
        } else {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.fillCheck.visibility = View.GONE
        }

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
                when {
                    isRecantVideo -> {
                        ReVideoPlayerActivity.pipStatus = 1
                        sendIntent(pos = position, ref = "RecantVideo")
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
                dialog.dismiss()
            }

            bindingMf.shareBtn.setOnClickListener {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND_MULTIPLE
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoReList[position].path))
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Get the list of apps that can handle the intent
                val packageManager = context.packageManager
                val resolvedActivityList = packageManager.queryIntentActivities(shareIntent, 0)
                val excludedComponents = mutableListOf<ComponentName>()


                // Iterate through the list and exclude your app
                for (resolvedActivity in resolvedActivityList) {
                    if (resolvedActivity.activityInfo.packageName == context.packageName) {
                        excludedComponents.add(ComponentName(resolvedActivity.activityInfo.packageName, resolvedActivity.activityInfo.name))
                    }
                }

                // Create a chooser intent
                val chooserIntent = Intent.createChooser(shareIntent, "Sharing Video")

                // Exclude your app from the chooser intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
                }

                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ContextCompat.startActivity(context, chooserIntent, null)
                dialog.dismiss()
            }

            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF =
                    LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(videoReList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(videoReList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            videoReList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation : ") }.append(videoReList[position].path)
                bindingIF.detailTV.text = infoText

            }

        }

    }

    override fun getItemCount(): Int {
        return videoReList.size
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
                                    val video = videoReList[position]
                                    val file = File(video.path)

                                    if (file.exists() && file.delete()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                        videoReList.removeAt(position)
                                    }
                                }

                                selectedItems.clear()
                                mode?.finish()
                                notifyDataSetChanged()
                                // Notify listener of the file count change
                                fileCountChangeListener.onFileCountChanged(videoReList.size)
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

    // Function to show confirmation dialog for single video deletion
    private fun showSingleDeleteConfirmation(position: Int) {
        val video = videoReList[position]

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
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteVideo(position: Int) {
        val video = videoReList[position]
        val file = File(video.path)

        if (file.exists() && file.delete()) {
            MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
            videoReList.removeAt(position)
            notifyItemRemoved(position)
            notifyDataSetChanged()
            Toast.makeText(context, "Video deleted successfully", Toast.LENGTH_SHORT).show()

            // If action mode is active, finish it after deletion
            actionMode?.finish()
            // Notify listener of the file count change
            fileCountChangeListener.onFileCountChanged(videoReList.size)
        } else {
            Toast.makeText(context, "Failed to delete video", Toast.LENGTH_SHORT).show()
        }
    }
    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()
        for (position in selectedItems) {
            val music = videoReList[position]
            val file = File(music.path)
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
            uris.add(fileUri)
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "video/*"
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



    @SuppressLint("NotifyDataSetChanged")
    fun updateRecentVideos(recentVideos: List<RecantVideo>) {
        videoReList.clear()
        videoReList.addAll(recentVideos)
        notifyDataSetChanged()
    }


    private fun sendIntent(pos: Int, ref: String) {
        ReVideoPlayerActivity.position = pos
        val intent = Intent(context, ReVideoPlayerActivity::class.java)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)

    }


}
