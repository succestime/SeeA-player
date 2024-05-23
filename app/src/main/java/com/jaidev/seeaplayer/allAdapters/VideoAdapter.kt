package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.Drawable
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
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.FoldersActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.PlayerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.GridVideoViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import java.io.File

class VideoAdapter(private val context: Context, private var videoList: ArrayList<VideoData>,private val isFolder: Boolean = false,private val isDownloadVideo: Boolean = false)
    : RecyclerView.Adapter<VideoAdapter.MyHolder>() {

    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private lateinit var sharedPreferences: SharedPreferences
    // Tracks selected items
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isGridMode = false // Track if grid mode is enabled

    companion object {
        private const val PREF_NAME = "video_titles"
        private const val VIEW_TYPE_GRID_VIDEO = 1
        private var videoRenameListener: VideoRenameListener? = null

    }
    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Load saved music titles
        loadVideoTitles()
    }

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    interface VideoDeleteListener {
        fun onVideoDeleted()


    }
    interface VideoRenameListener {
        fun onVideoRenamed(position: Int, newName: String)
    }

    private var videoDeleteListener: VideoDeleteListener? = null

    fun setVideoDeleteListener(listener: VideoDeleteListener) {
        videoDeleteListener = listener
    }
    // Function to update layout mode dynamically
    @SuppressLint("NotifyDataSetChanged")
    fun enableGridMode(enable: Boolean) {
        isGridMode = enable
        notifyDataSetChanged()
    }
//    interface VideoViewHolderBinder {
//        fun bind(videoData: VideoData)
//    }

    class MyHolder(binding: GridVideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val more = binding.MoreChoose
        val root = binding.root
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
      return MyHolder(GridVideoViewBinding.inflate(LayoutInflater.from(context), parent, false))

}

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = videoList[position].title

        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
            .into(holder.image)
        setIconTint(holder.more)
        // Determine if the item is currently selected
        if (selectedItems.contains(position)) {
            // Set your custom selected background on the root view of the item
            holder.root.setBackgroundResource(R.drawable.browser_selected_background)
        } else {
            // Reset to default background based on app theme
            val defaultBackgroundColor = getDefaultBackgroundColor()
            holder.root.setBackgroundColor(defaultBackgroundColor)
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
                // Otherwise, play the video without enabling selection
                when {

                    isFolder -> {
                        PlayerActivity.pipStatus = 1
                        sendIntent(pos = position, ref = "FoldersActivity")
                    }
                    MainActivity.search -> {
                        PlayerActivity.pipStatus = 2
                        sendIntent(pos = position, ref = "SearchVideos")
                    }
//                    isDownloadVideo -> {
//                        PlayerActivity.pipStatus = 3
//                        sendIntent(pos = position , ref = "LinkTubeDownload")
//
//                    }
                    videoList[position].id == PlayerActivity.nowPlayingId -> {
                        sendIntent(pos = position, ref = "NowPlaying")
                    }

                    else -> {
                        // Only play the video without enabling selection
                        sendIntent(pos = position, ref = "NormalClick")
                    }
                }
            }
        }

        holder.more.setOnClickListener {
            newPosition = position

            // Inflate the custom dialog layout
            val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, holder.root, false)
            val bindingMf = VideoMoreFeaturesBinding.bind(customDialog)
            // Create the dialog
            val dialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(customDialog)
            val dialog = dialogBuilder.create()

            // Show the dialog
            dialog.show()

            // Get the window attributes of the dialog
            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set dialog width and height
            window?.setGravity(Gravity.BOTTOM) // Set dialog gravity to bottom

            // Set click listeners for dialog buttons
            bindingMf.shareBtn.setOnClickListener {
                dialog.dismiss()

                // Create an ACTION_SEND intent to share the video
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
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
                startActivity(context, chooserIntent, null)
            }


            bindingMf.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF = LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context)
                    .setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()

                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName : ") }
                    .append(videoList[position].title)
                    .bold { append("\n\nDuration : ") }
                    .append(DateUtils.formatElapsedTime(videoList[position].duration / 1000))
                    .bold { append("\n\nFile Size : ") }.append(
                        Formatter.formatShortFileSize(context, videoList[position].size.toLong())
                    )
                    .bold { append("\n\nLocation : ") }.append(videoList[position].path)
                bindingIF.detailTV.text = infoText
            }

            bindingMf.renameBtn.setOnClickListener {
                dialog.dismiss()
                showRenameDialog(position, videoList[position].title)
            }

            bindingMf.deleteBtn.setOnClickListener {
                dialog.dismiss()

                val alertDialogBuilder = AlertDialog.Builder(context)
                val view = layoutInflater.inflate(R.layout.delete_alertdialog, null)

                val videoNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
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
                    .load(videoList[position].artUri)
                    .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
                    .into(iconImageView)

                videoNameDelete.text = videoList[position].title

                alertDialogBuilder.setView(view)

                val alertDialog = alertDialogBuilder.create()

                deleteText.setOnClickListener {
                    val file = File(videoList[position].path)
                    if (file.exists() && file.delete()) {
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        when {
                            MainActivity.search -> {
                                MainActivity.dataChanged = true
                                videoList.removeAt(position)
                                notifyDataSetChanged()
                            }
                            isFolder -> {
                                MainActivity.dataChanged = true
                                FoldersActivity.currentFolderVideos.removeAt(position)
                                notifyDataSetChanged()
                                videoDeleteListener?.onVideoDeleted()
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
        }

    }






    override fun getItemCount(): Int {
        return videoList.size
    }

    private fun showRenameDialog(position: Int, defaultName: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
        val editText = view.findViewById<EditText>(R.id.renameField)
        editText.setText(defaultName) // Set default text as current music title


        dialogBuilder.setView(view)
            .setTitle("Rename Video")
            .setMessage("Enter new name for the video:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != defaultName) {
                    renameVideo(position, newName)
                    dialogRF.dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        dialogRF = dialogBuilder.create()
        dialogRF.show()

    }
    private fun renameVideo(position: Int, newName: String) {
        val music = videoList[position]
        music.title = newName
        notifyItemChanged(position)
        saveVideoTitles(music.id, newName)
        val defaultTitle = music.title
        showRenameDialog(position, defaultTitle)
    }

    // Save updated video titles to SharedPreferences
    private fun saveVideoTitles(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }

    // Load saved video titles from SharedPreferences
    private fun loadVideoTitles() {
        for (video in videoList) {
            val savedTitle = sharedPreferences.getString(video.id, null)
            savedTitle?.let {
                video.title = it
            }
        }
    }



    // Function to get default background color based on app theme
    private fun getDefaultBackgroundColor(): Int {
        val isDarkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (isDarkMode) {
            // Dark mode is enabled, return dark color resource
            ContextCompat.getColor(context, R.color.gray)
        } else {
            // Light mode is enabled, return light color resource
            ContextCompat.getColor(context, R.color.white)
        }
    }
    fun updateVideoTitle(position: Int, newName: String) {
        videoList[position].title = newName
        notifyItemChanged(position)
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
            mode?.menuInflater?.inflate(R.menu.multiple_player_select_menu, menu)
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
            // Handle action mode menu items
            val actionMode = mode
            when (item?.itemId) {
                R.id.renameMulti -> {
                    // Call the showRenameDialog method here
                    if (selectedItems.size == 1) {
                        val selectedPosition = selectedItems.first()
                        val defaultName = videoList[selectedPosition].title
                      showRenameDialog(selectedPosition, defaultName)
                    } else {
                        Toast.makeText(context, "Please select only one video to rename", Toast.LENGTH_SHORT).show()
                    }
                    // Dismiss action mode
                    actionMode?.finish()
                    return true
                }

                R.id.shareMulti -> {
                    shareSelectedFiles()
                    // Dismiss action mode
                    actionMode?.finish()
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
                                    val video = videoList[position]
                                    val file = File(video.path)

                                    if (file.exists() && file.delete()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                        videoList.removeAt(position)
                                    }
                                }

                                selectedItems.clear()
                                mode?.finish()
                                notifyDataSetChanged()
                                // Dismiss action mode
                                actionMode?.finish()
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
            val music = videoList[position]
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(music.path)
            )
            uris.add(fileUri)
        }

        // Create an ACTION_SEND intent to share multiple files
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "video/*"
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
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
        val chooserIntent = Intent.createChooser(shareIntent, "Share Files")

        // Exclude your app from the chooser intent
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())

        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooserIntent)

        // Dismiss action mode
        actionMode?.finish()
    }





    private fun sendIntent(pos: Int, ref: String) {
        PlayerActivity.position = pos
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)
        startActivity(context, intent, null)

    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<VideoData>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }



    private fun setIconTint(imageView: ImageView) {
        // Get the drawable

     val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.icon_dark)

        // Get the theme color for the icon tint
        val iconTint = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.black)
        }

        // Set the tint for the drawable
        drawable?.let {
            DrawableCompat.setTint(it, iconTint)
        }

        // Set the modified drawable to your ImageView or wherever you're using it
        imageView.setImageDrawable(drawable)
    }
}

