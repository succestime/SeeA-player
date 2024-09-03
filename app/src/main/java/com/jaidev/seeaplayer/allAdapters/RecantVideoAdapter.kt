
package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.TypedValue
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MP3ConverterActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.MP3AppDatabase
import com.jaidev.seeaplayer.dataClass.MP3FileData
import com.jaidev.seeaplayer.dataClass.MP3FileEntity
import com.jaidev.seeaplayer.dataClass.RecantVideo
import com.jaidev.seeaplayer.databinding.RecantDownloadViewBinding
import com.jaidev.seeaplayer.recantFragment.DaysDownload
import com.jaidev.seeaplayer.recantFragment.ReVideoPlayerActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.UUID

class RecentVideoAdapter(private val context: Context,
                         private var videoReList: ArrayList<RecantVideo> ,
                         private val isRecantVideo: Boolean = false,
                         val fileCountChangeListener: OnFileCountChangeListener,
) :
    RecyclerView.Adapter<RecentVideoAdapter.MyAdapter>() {
    interface OnFileCountChangeListener {
        fun onFileCountChanged(newCount: Int)
    }
    private  var newPosition = 0
    private val selectedItems = HashSet<Int>()
    private var actionMode: ActionMode? = null
    private var isSelectionModeEnabled = false // Flag to track whether selection mode is active
    private var isAllSelected = false // Add this flag


    companion object {
        @SuppressLint("StaticFieldLeak")
        private var adapterInstance: RecentVideoAdapter? = null

        fun updateNewIndicator(videoId: String) {
            adapterInstance?.let { adapter ->
                val index = adapter.videoReList.indexOfFirst { it.id == videoId }
                if (index != -1) {
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    init {
        adapterInstance = this
    }




    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionModeEnabled = false
        selectedItems.clear()
        actionMode?.finish() // Finish the ActionMode if it's active
        actionMode = null
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }




    class MyAdapter(binding: RecantDownloadViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val more = binding.MoreChoose
        val root = binding.root
        val emptyCheck = binding.emptyCheck
        val fillCheck = binding.fillCheck
        val newIndicator = binding.newIndicator

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
        val video = videoReList[position]

        holder.title.text = videoReList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoReList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoReList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.image)

        // Check if the video has been played
        if (isVideoPlayed(video.id)) {
            holder.newIndicator.visibility = View.GONE
        } else {
            holder.newIndicator.visibility = View.VISIBLE

        }

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
                        markVideoAsPlayed(video.id) // Mark video as played
                        notifyItemChanged(position) // Update the item

                    }
                }
            }
        }
        holder.more.setOnClickListener {
            showBottomSheetDialog(video, position)

        }

    }


    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "ObsoleteSdkInt")
    private fun showBottomSheetDialog(video: RecantVideo, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.re_video_more_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        view.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

        val textTitle = view.findViewById<TextView>(R.id.textTitle)
        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        val playButton = view.findViewById<LinearLayout>(R.id.playButton)
        val shareButton = view.findViewById<LinearLayout>(R.id.shareButton)
        val propertiesButton = view.findViewById<LinearLayout>(R.id.propertiesButton)
        val convertToMP3Button = view.findViewById<LinearLayout>(R.id.convertToMP3Button)


        textTitle.text = video.title

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
//                            val dao = DatabaseClient.getInstance(context).playlistDao()
//
//                            // Create a new playlist entity
//                            val newPlaylist = PlaylistEntity(
//                                name = playlistName
//                            )
//
//                            // Insert the new playlist into the database and get its ID
//                            val playlistId = dao.insertPlaylist(newPlaylist)
//
//                            // Add the selected song to the newly created playlist
//                            val crossRef = PlaylistVideoCrossRef(
//                                playlistId = playlistId,
//                                videoId = video.id // Assuming playlist is of type Music
//                            )
//                            dao.insertPlaylistVideoCrossRef(crossRef)
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


//         Handle Convert to MP3 button click
        convertToMP3Button.setOnClickListener {
            val bottomSheetMP3Dialog = BottomSheetDialog(context)
            val mp3View = LayoutInflater.from(context).inflate(R.layout.mp3_converter_layout, null)

            bottomSheetMP3Dialog.setContentView(mp3View)
            bottomSheetMP3Dialog.setCancelable(false) // This line makes the dialog non-dismissible

            mp3View.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

            // Initialize UI components in the MP3 conversion view
            val convertingTextView = mp3View.findViewById<TextView>(R.id.convertingTextView)
            val titleTextView = mp3View.findViewById<TextView>(R.id.titleTextView)
            val percentageTextView = mp3View.findViewById<TextView>(R.id.percentageTextView)
            val cancelButton = mp3View.findViewById<TextView>(R.id.cancelButton)
            val progressBar = mp3View.findViewById<ProgressBar>(R.id.progressBar)
            val convertingView = mp3View.findViewById<RelativeLayout>(R.id.convertingView)
            val completeBottomSheet = mp3View.findViewById<ConstraintLayout>(R.id.complete_bottomSheet)
            val videoPathAndTitle = mp3View.findViewById<TextView>(R.id.videoPathAndTitle)
            val openButton = mp3View.findViewById<Button>(R.id.openButton)
            val cancel1Button = mp3View.findViewById<Button>(R.id.cancel1Button)

            bottomSheetDialog.dismiss()
            bottomSheetMP3Dialog.show()

            openButton.setOnClickListener {
                val intent = Intent(context, MP3ConverterActivity::class.java)
                context.startActivity(intent)
                bottomSheetMP3Dialog.dismiss()
            }

            cancel1Button.setOnClickListener {
                bottomSheetMP3Dialog.dismiss()
            }

            val inputFilePath = video.path
            val baseName = video.title.substringBeforeLast(".")
            val customDirectoryName = "SeeA MP3 Audio"

            // Initialize outputFilePath based on Android version
            val outputFilePath: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${context.getExternalFilesDir(null)}/$baseName.mp3"
            } else {
                val customDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), customDirectoryName)
                if (!customDirectory.exists()) {
                    customDirectory.mkdirs()
                }
                "${customDirectory.path}/$baseName.mp3"
            }

            convertingView.visibility = View.VISIBLE
            completeBottomSheet.visibility = View.GONE

            var attempt = 0
            val maxRetries = 3

            // Define the onConversionSuccess function
            fun onConversionSuccess() {
                convertingTextView.text = "Conversion Complete!"
                progressBar.progress = 100
                percentageTextView.text = "100%"

                val file = File(outputFilePath)
                val fileSize = formatFileSize(file.length())
                val dateAdded = System.currentTimeMillis()
                val duration = getMP3Duration(outputFilePath)

                // Insert the file into the MediaStore or custom directory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "$baseName.mp3")
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$customDirectoryName")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            file.inputStream().copyTo(outputStream)
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                } else {
                    val customDirectory = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MUSIC), customDirectoryName)
                    if (!customDirectory.exists()) {
                        customDirectory.mkdirs()
                    }
                    file.renameTo(File("${customDirectory.path}$baseName.mp3"))
                }

                convertingView.visibility = View.GONE
                completeBottomSheet.visibility = View.VISIBLE
                videoPathAndTitle.text = "Saved to : SeeA MP3 Converter /$customDirectoryName/\n$baseName.mp3"

                val mp3FileData = MP3FileData(
                    id = UUID.randomUUID().toString(),
                    title = "$baseName.mp3",
                    duration = duration,
                    size = fileSize,
                    dateAdded = dateAdded,
                    path = outputFilePath,
                    artUri = video.artUri.toString()

                )

                GlobalScope.launch(Dispatchers.IO) {
                    val db = MP3AppDatabase.getDatabase(context)
                    db.mp3FileDao().insert(
                        MP3FileEntity(
                            id = mp3FileData.id,
                            title = mp3FileData.title,
                            duration = mp3FileData.duration,
                            size = mp3FileData.size,
                            dateAdded = mp3FileData.dateAdded,
                            path = mp3FileData.path,
                            artUri = mp3FileData.artUri // Pass the artUri from the video

                        )
                    )
                }

                if (context is MP3ConverterActivity) {
                    context.addMP3File(mp3FileData )
                }
            }

            // Define the onConversionFailed function
            fun onConversionFailed() {
                convertingTextView.text = "Conversion Failed!"
                Toast.makeText(context, "This video file is already converted or some problem is coming", Toast.LENGTH_LONG).show()

            }

            // Function to handle the conversion process
            fun convertVideoToMP3() {
                GlobalScope.launch(Dispatchers.IO) {
                    attempt++
                    val ffmpegCommand = arrayOf("-i", inputFilePath, "-q:a", "0", "-map", "a", outputFilePath)
                    val rc = FFmpeg.execute(ffmpegCommand)

                    val typedValue = TypedValue()
                    val theme = context.theme
                    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                    val defaultTextColor = ContextCompat.getColor(context, typedValue.resourceId)

                    withContext(Dispatchers.Main) {
                        if (rc == Config.RETURN_CODE_SUCCESS) {
                            onConversionSuccess()
                        } else {
                            if (attempt < maxRetries) {
                                convertVideoToMP3()  // Retry
                                convertingTextView.setTextColor(defaultTextColor)
                                titleTextView.setTextColor(defaultTextColor)
                            } else {
                                onConversionFailed()
                                // Set text colors to red on failure
                                convertingTextView.setTextColor(Color.RED)
                                titleTextView.setTextColor(Color.RED)
                            }
                        }
                    }
                }
            }

            // Start the conversion process
            convertVideoToMP3()

            // Enable statistics callback to update progress
            Config.enableStatisticsCallback { stats ->
                GlobalScope.launch(Dispatchers.Main) {
                    val progress = (stats.time.toFloat() / video.duration) * 100
                    progressBar.progress = progress.toInt()
                    percentageTextView.text = "${progress.toInt()}%"

                    // Ensure the final update if conversion reaches 100%
                    if (progress >= 100) {
                        progressBar.progress = 100
                        percentageTextView.text = "100%"
                    }
                }
            }



            cancelButton.setOnClickListener {
                FFmpeg.cancel()
                bottomSheetMP3Dialog.dismiss()
            }
        }



        deleteButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                showPermissionRequestDialog()
            } else {
                showDeleteDialog(position)
            }
        }

        shareButton.setOnClickListener{
            bottomSheetDialog.dismiss()
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
            shareIntent.type = "video/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(video.path))
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
            fileNameTextView.text = videoReList[position].title
            durationTextView.text = DateUtils.formatElapsedTime(videoReList[position].duration / 1000)
            // Ensure video.size is properly converted to a numeric type
            val sizeInBytes = video.size.toLongOrNull() ?: 0L
            val formattedSize = Formatter.formatShortFileSize(context, sizeInBytes)
            val bytesWithCommas = NumberFormat.getInstance().format(sizeInBytes)
            sizeTextView.text = "$formattedSize ($bytesWithCommas bytes)"



            locationTextView.text = videoReList[position].path

            val dialogIF = MaterialAlertDialogBuilder(context)
                .setView(customDialogIF)
                .setCancelable(false)
                .create()
            positiveButton.setOnClickListener {
                dialogIF.dismiss()
            }
            dialogIF.show()
        }

        playButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            sendIntent(pos = position, ref = "VideoMoreAdapter")
        }

        bottomSheetDialog.show()
    }
    private fun formatFileSize(sizeInBytes: Long): String {
        val kb = sizeInBytes / 1024
        val mb = kb / 1024
        return when {
            mb > 0 -> "${mb} MB"
            kb > 0 -> "${kb} KB"
            else -> "${sizeInBytes} B"
        }
    }

    // Function to get MP3 duration
    private fun getMP3Duration(filePath: String): Long {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        return try {
            mediaMetadataRetriever.setDataSource(filePath)
            val durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            mediaMetadataRetriever.release()
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
        actionMode?.title = "${selectedItems.size} / ${videoReList.size} Selected"
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
                R.id.checkMulti -> {
                    toggleSelectAllItems(item)

                }

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
                                        DaysDownload.updateEmptyViewVisibility()

                                    }
                                }

                                selectedItems.clear()
                                notifyDataSetChanged()
                                updateActionModeTitle()
                                DaysDownload.updateEmptyViewVisibility()
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

        @SuppressLint("NotifyDataSetChanged")
        private fun toggleSelectAllItems(item: MenuItem) {
            isAllSelected = if (isAllSelected) {
                // Unselect all items
                selectedItems.clear()
                item.setIcon(R.drawable.round_crop_square_24)
                false
            } else {
                // Select all items
                for (i in 0 until videoReList.size) {
                    selectedItems.add(i)
                }
                item.setIcon(R.drawable.check_box_24)
                true
            }
            updateActionModeTitle()
            notifyDataSetChanged()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showDeleteDialog(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.delete_alertdialog, null)

        val videoNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        val cancelText = view.findViewById<TextView>(R.id.cancelText)
        val iconImageView = view.findViewById<ImageView>(R.id.videoImage)

        deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

        cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

        Glide.with(context)
            .asBitmap()
            .load(videoReList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(iconImageView)

        videoNameDelete.text = videoReList[position].title

        alertDialogBuilder.setView(view)
            .setCancelable(false)
        val alertDialog = alertDialogBuilder.create()

        deleteText.setOnClickListener {
            deleteVideo(position)
            alertDialog.dismiss()

        }
        cancelText.setOnClickListener {
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
        val video = videoReList[position]
        val file = File(video.path)

        if (file.exists() && file.delete()) {
            MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
            videoReList.removeAt(position)
            notifyItemRemoved(position)
            notifyDataSetChanged()
            // If action mode is active, finish it after deletion
            actionMode?.finish()
            DaysDownload.updateEmptyViewVisibility()
            fileCountChangeListener.onFileCountChanged(videoReList.size)
        } else {
            Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
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
    private fun markVideoAsPlayed(videoId: String) {
        val sharedPreferences = context.getSharedPreferences("played_videos", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(videoId, true)
            apply()
        }
    }

    private fun isVideoPlayed(videoId: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("played_videos", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(videoId, false)
    }

}

