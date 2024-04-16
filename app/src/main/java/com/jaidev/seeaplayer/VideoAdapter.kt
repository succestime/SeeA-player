package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
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
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.DetailsViewBinding
import com.jaidev.seeaplayer.databinding.GridVideoViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import com.jaidev.seeaplayer.databinding.VideoViewBinding
import java.io.File

class VideoAdapter(private val context: Context, private var videoList: ArrayList<VideoData>,private val isFolder: Boolean = false,)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private lateinit var sharedPreferences: SharedPreferences
    // Tracks selected items
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isGridMode = false // Track if grid mode is enabled

    companion object {
        private const val PREF_NAME = "video_titles"
        private const val VIEW_TYPE_VIDEO = 0
        private const val VIEW_TYPE_GRID_VIDEO = 1
    }
    init {
        sharedPreferences = context.getSharedPreferences(VideoAdapter.PREF_NAME, Context.MODE_PRIVATE)
        // Load saved music titles
        loadVideoTitles()
    }

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    interface VideoDeleteListener {
        fun onVideoDeleted()


    }
    private var videoDeleteListener: VideoDeleteListener? = null

    fun setVideoDeleteListener(listener: VideoDeleteListener) {
        videoDeleteListener = listener
    }
    // Function to update layout mode dynamically
    fun enableGridMode(enable: Boolean) {
        isGridMode = enable
        notifyDataSetChanged()
    }
    inner class VideoViewHolder(private val binding: VideoViewBinding) :
        RecyclerView.ViewHolder(binding.root) ,VideoViewHolderBinder {

        @SuppressLint("NotifyDataSetChanged", "SuspiciousIndentation")
        override  fun bind(videoData: VideoData) {
            binding.apply {
                videoName.text = videoData.title
                duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
                // Load image using Glide
                Glide.with(context)
                    .asBitmap()
                    .load(videoList[position].artUri)
                    .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
                    .into(videoImage)

                setIconTint(MoreChoose)
                root.setOnLongClickListener {
                    toggleSelection(position)
                    true
                }
                root.setOnClickListener {
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


                // Show/hide multi-select icon based on selection
                multiIcon.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE


                MoreChoose.setOnClickListener {
                    newPosition = position

                    // Inflate the custom dialog layout
                    val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, root, false)
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
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "video/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                        startActivity(context, Intent.createChooser(shareIntent, "Sharing Video"), null)
                    }

                    bindingMf.infoBtn.setOnClickListener {
                        dialog.dismiss()
                        val customDialogIF = LayoutInflater.from(context).inflate(R.layout.details_view, root, false)
                        val bindingIF = DetailsViewBinding.bind(customDialogIF)
                        val dialogIF = MaterialAlertDialogBuilder(context)
                            .setView(customDialogIF)
                            .setCancelable(false)
                            .setPositiveButton("OK") { self, _ ->
                                self.dismiss()
                            }
                            .create()

//                        dialogIF.window?.setGravity(Gravity.BOTTOM) // Set the gravity of the dialog window to bottom
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


                        // Get the current video title as default text
                        val defaultTitle = videoList[position].title

                        // Show the rename dialog with the current video title as default text
                        showRenameDialog(position, defaultTitle)
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
        }
    }

    inner class GridVideoViewHolder(private val binding: GridVideoViewBinding) :
        RecyclerView.ViewHolder(binding.root),VideoViewHolderBinder{
         override fun bind(videoData: VideoData) {
            binding.apply {
                videoName.text = videoData.title
                duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)

                Glide.with(context)
                    .asBitmap()
                    .load(videoList[position].artUri)
                    .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
                    .into(videoImage)

                setIconTint(MoreChoose)
                root.setOnLongClickListener {
                    toggleSelection(position)
                    true
                }
                root.setOnClickListener {
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


//                // Show/hide multi-select icon based on selection
//                multiIcon.visibility = if (selectedItems.contains(position)) View.VISIBLE else View.GONE


                MoreChoose.setOnClickListener {
                    newPosition = position

                    // Inflate the custom dialog layout
                    val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, root, false)
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
                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "video/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                        startActivity(context, Intent.createChooser(shareIntent, "Sharing Video"), null)
                    }

                    bindingMf.infoBtn.setOnClickListener {
                        dialog.dismiss()
                        val customDialogIF = LayoutInflater.from(context).inflate(R.layout.details_view, root, false)
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

                        // Get the current video title as default text
                        val defaultTitle = videoList[position].title

                        // Show the rename dialog with the current video title as default text
                        showRenameDialog(position, defaultTitle)
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
        }
    }
    override fun getItemViewType(position: Int): Int {
        return if (isFolder) VIEW_TYPE_GRID_VIDEO else VIEW_TYPE_VIDEO
    }

    interface VideoViewHolderBinder {
        fun bind(videoData: VideoData)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VIDEO -> {
                val binding = VideoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                VideoViewHolder(binding)
            }
            VIEW_TYPE_GRID_VIDEO -> {
                val binding = GridVideoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GridVideoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val videoData = videoList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_VIDEO -> {
                (holder as VideoViewHolder).bind(videoData)
            }
           else -> {
                (holder as GridVideoViewHolder).bind(videoData)
            }
        }
    }





    override fun getItemCount(): Int {
        return videoList.size
    }





    // Toggle selection for multi-select
    // Toggle selection for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        // Start or finish action mode based on selection
        if (selectedItems.isEmpty()) {
            actionMode?.finish()
        } else {
            startActionMode()
        }

        // Update selected items
        notifyDataSetChanged()
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
            mode?.menuInflater?.inflate(R.menu.multiple_select_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Hide the menu_rename item if more than one item is selected
//            val renameItem = menu?.findItem(R.id.shareMultiBrowser)
//            renameItem?.isVisible = selectedItems.size == 1

            return true
        }


        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            // Handle action mode menu items
            val actionMode = mode
            when (item?.itemId) {
                R.id.shareMultiBrowser -> {
                    deleteSelectedVideos(actionMode)
                    return true
                }
                R.id.deleteMultiBrowser -> {
// Call the showRenameDialog method here
                    if (selectedItems.size == 1) {
                        val selectedPosition = selectedItems.first()
                        val defaultName = videoList[selectedPosition].title
                        showRenameDialog(selectedPosition, defaultName)
                    } else {
                        Toast.makeText(context, "Please select only one video to rename", Toast.LENGTH_SHORT).show()
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
            notifyDataSetChanged()
        }
    }
    private fun deleteSelectedVideos(actionMode: ActionMode?) {
        val selectedPositions = ArrayList(selectedItems)
        val deleteDialogBuilder = AlertDialog.Builder(context)
        val deleteView = layoutInflater.inflate(R.layout.delete_alertdialog, null)

        val deleteText = deleteView.findViewById<TextView>(R.id.deleteText)
        val cancelText = deleteView.findViewById<TextView>(R.id.cancelText)
        val iconImageView = deleteView.findViewById<ImageView>(R.id.videoImage)
        val videoNameDelete = deleteView.findViewById<TextView>(R.id.videmusicNameDelete)

        // Set the delete text color to red
        deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

        // Set the cancel text color to black
        cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Load video image into iconImageView using Glide
        Glide.with(context)
            .asBitmap()
            .load(videoList[selectedPositions.first()].artUri) // Assuming only one item is selected
            .apply(RequestOptions().placeholder(R.mipmap.ic_logo_o).centerCrop())
            .into(iconImageView)

        // Set the video name
        videoNameDelete.text = videoList[selectedPositions.first()].title // Assuming only one item is selected

        deleteDialogBuilder.setView(deleteView)

        val deleteDialog = deleteDialogBuilder.create()

        deleteText.setOnClickListener {
            for (position in selectedPositions) {
                val file = File(videoList[position].path)
                if (file.exists() && file.delete()) {
                    MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                    when {
                        MainActivity.search -> {
                            MainActivity.dataChanged = true
                            videoList.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        isFolder -> {
                            MainActivity.dataChanged = true
                            FoldersActivity.currentFolderVideos.removeAt(position)
                            notifyItemRemoved(position)
                            videoDeleteListener?.onVideoDeleted()
                        }
                    }
                } else {
                    Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
                }
            }
            deleteDialog.dismiss()
            actionMode?.finish()
            // You might want to call updateTotalVideoCount() here to refresh the total video count
        }

        cancelText.setOnClickListener {
            // Handle cancel action here
            deleteDialog.dismiss()
        }
        deleteDialog.show()
    }


    // Update the total video count interface method

    private fun renameMusic(position: Int, newName: String) {
        val music = videoList[position]
        val uniqueIdentifier = music.id // or music.path, depending on what is unique
        music.path = newName
        notifyItemChanged(position)
        saveMusicTitle(uniqueIdentifier, newName)
        val defaultTitle = music.title
        showRenameDialog(position, defaultTitle)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showRenameDialog(position: Int, defaultTitle: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
        val editText = view.findViewById<EditText>(R.id.renameField)
        editText.setText(defaultTitle) // Set default text as current music title

        dialogBuilder.setView(view)
            .setTitle("Rename Video")
            .setMessage("Enter new name for the video:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                //val newName = editText.text.toString().trim()
                val currentFile = File(videoList[position].path)
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameMusic(position, newName)

                    if (currentFile.exists() && newName.toString().isNotEmpty()) {
                        val newFile = File(
                            currentFile.parentFile,
                            newName.toString() + "." + currentFile.extension
                        )
                        if (currentFile.renameTo(newFile)) {
                            MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()),
                                arrayOf("video/*"), null)
                            when {

                                isFolder -> {
                                    FoldersActivity.currentFolderVideos[position].title = newName.toString()
                                    FoldersActivity.currentFolderVideos[position].path = newFile.path
                                    FoldersActivity.currentFolderVideos[position].artUri = Uri.fromFile(newFile)
                                    MainActivity.dataChanged = true
                                    videoDeleteListener?.onVideoDeleted()
                                    notifyItemChanged(position)
                                }
                                else -> {
                                    MainActivity.videoList[position].title = newName.toString()
                                    MainActivity.videoList[position].path = newFile.path
                                    MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
                                    MainActivity.dataChanged = true
                                    videoDeleteListener?.onVideoDeleted()
                                    notifyItemChanged(position)
                                }
                            }
                        } else {
                            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

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


    }
    private fun saveMusicTitle(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }


    private fun loadVideoTitles() {
        for (music in videoList) {
            val savedTitle = sharedPreferences.getString(music.id, null)
            savedTitle?.let {
                music.title = it
            }
        }
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


