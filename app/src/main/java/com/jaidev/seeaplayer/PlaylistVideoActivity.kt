package com.jaidev.seeaplayer


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat.clearColorFilter
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.jaidev.seeaplayer.allAdapters.PlaylistVideoShowAdapter
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity.Companion.adapter
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.DatabaseClient
import com.jaidev.seeaplayer.dataClass.PlaylistVideoCrossRef
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.dataClass.VideoEntity
import com.jaidev.seeaplayer.databinding.ActivityPlaylistVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistVideoActivity : AppCompatActivity() , PlaylistVideoShowAdapter.OnSelectionChangeListener{
    lateinit var binding: ActivityPlaylistVideoBinding
    private lateinit var videoAdapter: PlaylistVideoShowAdapter
    private val db by lazy { DatabaseClient.getInstance(this) }
    private var playlistId: Long = -1
    private var isClick: Boolean = false // Flag for repeat mode
    private var isClickShuffle: Boolean = false // Flag for repeat mode
    private val videoList = mutableListOf<VideoData>()
    private var isAllSelected = false
    private var isActionModeEnabled = false

    private val updatePlaylistReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val receivedPlaylistId = intent?.getLongExtra("playlistId", -1)
            if (receivedPlaylistId == playlistId) {
                lifecycleScope.launch {
                    loadVideosFromDatabase()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlaylistVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        playlistId = intent.getLongExtra("playlistId", -1)
        if (playlistId == -1L) finish() // Invalid playlist ID

        LocalBroadcastManager.getInstance(this).registerReceiver(updatePlaylistReceiver, IntentFilter("UPDATE_PLAYLIST_VIDEOS"))

        setupRecyclerView()
        loadVideosFromDatabase()
        // Set up the navigation icon click listener to finish the activity
        binding.playlistToolbar.setNavigationOnClickListener {
            finish()
        }


        // Set up menu item click listener
        binding.playlistToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.morePlaylist -> {
                    // Handle the first menu item click
                    handleFirstMenuItemClick()
                    true
                }
                R.id.shortPlaylist -> {
                    // Handle the second menu item click
                    handleSecondMenuItemClick()
                    true
                }
                else -> false
            }
        }

        binding.addVideosButton.setOnClickListener {
            val bottomSheet = AddVideosBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
        setSwipeRefreshBackgroundColor()

        // Set up the repeat button click listener
        binding.isClick.setOnClickListener {
            toggleRepeatMode()
        }

        binding.shuffle.setOnClickListener {
            toggleShuffleMode()
        }
        binding.selectAll.setOnClickListener {
            if (isAllSelected) {
                // Deselect all items
                videoAdapter.clearSelection()
            } else {
                // Select all items
                videoAdapter.selectAll()
            }
            // Toggle the flag
            isAllSelected = !isAllSelected

            // Update the selectAll button icon
            updateSelectAllIcon(isAllSelected)
        }

        binding.playVideo.setOnClickListener {
            val selectedVideos = videoAdapter.getSelectedVideos()
            if (selectedVideos.isNotEmpty()) {
                val videoUris = selectedVideos.map { Uri.parse(it.artUri.toString()) } // Assuming `uri` is a property of `VideoData`
                val videoTitles = selectedVideos.map { it.title } // Assuming `title` is a property of `VideoData`

                val intent = Intent(this, PlayerFileActivity::class.java).apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videoUris))
                    putStringArrayListExtra("videoTitles", ArrayList(videoTitles))
                    putExtra("isRepeatMode", isClick) // Repeat mode flag
                    putExtra("isShuffleMode", isClickShuffle) // Shuffle mode flag
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No videos selected", Toast.LENGTH_SHORT).show()
            }
        }


        binding.removeVideo.setOnClickListener {
            val selectedVideos = videoAdapter.getSelectedVideos() // Get the selected videos
            val dialogView = layoutInflater.inflate(R.layout.playlist_video_selected_remove, null)

            // Update the message based on the number of selected items
            val messageTextView = dialogView.findViewById<TextView>(R.id.message)
            when (selectedVideos.size) {
                1 -> messageTextView.text = "Are you sure you want to remove this video?"
                else -> messageTextView.text = "Are you sure you want to remove these videos?"
            }

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog.dismiss() // Close the dialog when cancel is clicked
            }

            dialogView.findViewById<Button>(R.id.remove_button).setOnClickListener {
                dialog.dismiss() // Close the dialog and proceed with removal

                if (selectedVideos.isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            selectedVideos.forEach { video ->
                                db.playlistDao().deleteVideoFromPlaylist(playlistId, video.id)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            loadVideosFromDatabase()
                            loadVideosForPlaylist()
                            // Clear selection after removal
                            videoAdapter.clearSelection()
                            updatePlaylistName(0) // Reset selection count


                            // Disable selection mode if the playlist is empty
                            if (videoAdapter.getVideos().isEmpty()) {
                                videoAdapter.disableSelectionMode()
                            }

                            // Send broadcast to update PlaylistFolderActivity
                            val intent = Intent("UPDATE_PLAYLIST_FOLDER")
                            LocalBroadcastManager.getInstance(this@PlaylistVideoActivity).sendBroadcast(intent)
                        }
                    }
                } else {
                    Toast.makeText(this, "No videos selected to remove", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }



    }
    private fun updateSelectAllIcon(isSelected: Boolean) {
        if (isSelected) {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_box_24, 0, 0, 0)
        } else {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.round_crop_square_24, 0, 0, 0)
        }
    }



    private fun handleFirstMenuItemClick() {
        val playlistId = intent.getLongExtra("playlistId", -1)
        val bottomSheetFragment = MorePlaylistBottomSheetFragment.newInstance(playlistId)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun handleSecondMenuItemClick() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.playlist_sort_order_dialog, null)

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Get references to layout containers and text views
        val layoutSortTitle = dialogView.findViewById<LinearLayout>(R.id.layout_sort_title)
        val layoutSortDateAdded = dialogView.findViewById<LinearLayout>(R.id.layout_sort_date)
        val layoutSortDuration = dialogView.findViewById<LinearLayout>(R.id.layout_sort_length)
        val layoutSortSize = dialogView.findViewById<LinearLayout>(R.id.layout_sort_size)
        val tv_a_to_zLayout = dialogView.findViewById<LinearLayout>(R.id.tv_a_to_zLayout)
        val tv_z_to_aLayout = dialogView.findViewById<LinearLayout>(R.id.tv_z_to_aLayout)
        val shortestLayout = dialogView.findViewById<LinearLayout>(R.id.shortestLayout)
        val LongestLayout = dialogView.findViewById<LinearLayout>(R.id.LongestLayout)
        val OldestLayout = dialogView.findViewById<LinearLayout>(R.id.OldestLayout)
        val newestLayout = dialogView.findViewById<LinearLayout>(R.id.newestLayout)
        val SmallestLayout = dialogView.findViewById<LinearLayout>(R.id.SmallestLayout)
        val LargestLayout = dialogView.findViewById<LinearLayout>(R.id.LargestLayout)
        val cancel_btn = dialogView.findViewById<TextView>(R.id.cancel_btn)
        val btn_done = dialogView.findViewById<TextView>(R.id.btn_done)

        val ZtoAOption = dialogView.findViewById<LinearLayout>(R.id.ZtoAOption)
        val lengthOption = dialogView.findViewById<LinearLayout>(R.id.lengthOption)
        val dateOption = dialogView.findViewById<LinearLayout>(R.id.dateOption)
        val sizeOption = dialogView.findViewById<LinearLayout>(R.id.SizeOption)

        val tvSortTitleIcon = dialogView.findViewById<ImageView>(R.id.tv_sort_icon)
        val tvSortTitle = dialogView.findViewById<TextView>(R.id.tv_sort_title)
        val tvSortLengthIcon = dialogView.findViewById<ImageView>(R.id.tv_sort_length_I)
        val tvSortLength = dialogView.findViewById<TextView>(R.id.tv_sort_length)
        val tvSortDateIcon = dialogView.findViewById<ImageView>(R.id.tv_sort_date_I)
        val tvSortDate = dialogView.findViewById<TextView>(R.id.tv_sort_date)
        val tvSortSizeIcon = dialogView.findViewById<ImageView>(R.id.tv_sort_size_I)
        val tvSortSize = dialogView.findViewById<TextView>(R.id.tv_sort_size)
        val option1TextView = dialogView.findViewById<TextView>(R.id.option1TextView)
        val option2TextView = dialogView.findViewById<TextView>(R.id.option2TextView)
        val option3TextView = dialogView.findViewById<TextView>(R.id.option3TextView)
        val option4TextView = dialogView.findViewById<TextView>(R.id.option4TextView)
        val option5TextView = dialogView.findViewById<TextView>(R.id.option5TextView)
        val option6TextView = dialogView.findViewById<TextView>(R.id.option6TextView)
        val option7TextView = dialogView.findViewById<TextView>(R.id.option7TextView)
        val option8TextView = dialogView.findViewById<TextView>(R.id.option8TextView)
        val option1ImageView = dialogView.findViewById<ImageView>(R.id.option1ImageView)
        val option2ImageView = dialogView.findViewById<ImageView>(R.id.option2ImageView)
        val option3ImageView = dialogView.findViewById<ImageView>(R.id.option3ImageView)
        val option4ImageView = dialogView.findViewById<ImageView>(R.id.option4ImageView)
        val option5ImageView = dialogView.findViewById<ImageView>(R.id.option5ImageView)
        val option6ImageView = dialogView.findViewById<ImageView>(R.id.option6ImageView)
        val option7ImageView = dialogView.findViewById<ImageView>(R.id.option7ImageView)
        val option8ImageView = dialogView.findViewById<ImageView>(R.id.option8ImageView)

        // Initialize sorting option visibility
        ZtoAOption.visibility = View.GONE  // Make ZtoAOption visible by default
        lengthOption.visibility = View.GONE
        dateOption.visibility = View.GONE
        sizeOption.visibility = View.GONE

        fun clearSelection(excludeLayout: LinearLayout? = null) {
            // Get the default text color from the theme (android:attr/textColorPrimary)
            val typedValue = TypedValue()
            val theme = this.theme
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val defaultTextColor = ContextCompat.getColor(this, typedValue.resourceId)

            // Reset backgrounds, text colors, and clear icon color filters for all layouts
            listOf(
                Pair(tv_a_to_zLayout, Pair(option1TextView, option1ImageView)),
                Pair(tv_z_to_aLayout, Pair(option2TextView, option2ImageView)),
                Pair(shortestLayout, Pair(option3TextView, option3ImageView)),
                Pair(LongestLayout, Pair(option4TextView, option4ImageView)),
                Pair(OldestLayout, Pair(option5TextView, option5ImageView)),
                Pair(newestLayout, Pair(option6TextView, option6ImageView)),
                Pair(SmallestLayout, Pair(option7TextView, option7ImageView)),
                Pair(LargestLayout, Pair(option8TextView, option8ImageView))
            ).forEach { (layout, pair) ->
                if (layout != excludeLayout) {
                    val backgroundResId = when (layout) {
                        tv_a_to_zLayout, shortestLayout, OldestLayout, SmallestLayout -> R.drawable.combined_background_left
                        tv_z_to_aLayout, LongestLayout, newestLayout, LargestLayout -> R.drawable.combined_background_right
                        else -> R.drawable.combined_background_left // Default to left if not matched
                    }

                    layout.setBackgroundResource(backgroundResId)
                    pair.first.setTextColor(defaultTextColor)  // Set text color to android:textColorPrimary
                    pair.second.clearColorFilter()  // Clear the color filter
                }
            }
        }


        fun clearSelection() {
            // Get the default text color from the theme (android:attr/textColorPrimary)
            val typedValue = TypedValue()
            val theme = this.theme
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val defaultTextColor = ContextCompat.getColor(this, typedValue.resourceId)

            listOf(tvSortTitle, tvSortLength, tvSortDate, tvSortSize).forEach {
                it.setTextColor(defaultTextColor)
            }
            listOf(tvSortTitleIcon, tvSortLengthIcon, tvSortDateIcon, tvSortSizeIcon).forEach {
                it.clearColorFilter()
            }
        }



        // Function to handle sorting option selection
        fun selectOption(selectedLayout: LinearLayout, optionsToShow: List<LinearLayout>, icon: ImageView, textView: TextView) {
            ZtoAOption.visibility = View.GONE
            lengthOption.visibility = View.GONE
            dateOption.visibility = View.GONE
            sizeOption.visibility = View.GONE
            optionsToShow.forEach { it.visibility = View.VISIBLE }
            clearSelection()
            clearSelection(selectedLayout)
            textView.setTextColor(ContextCompat.getColor(this, R.color.cool_blue))
            icon.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
        }

        fun handleIndividualSelection(layout: LinearLayout, textView: TextView, imageView: ImageView, isLeftSide: Boolean, isSelected: Boolean) {
            val backgroundResId = if (isSelected) {
                if (isLeftSide) R.drawable.combined_background_left_cool_blue else R.drawable.combined_background_right_cool_blue
            } else {
                if (isLeftSide) R.drawable.combined_background_left else R.drawable.combined_background_right
            }

            clearSelection(layout)
            layout.setBackgroundResource(backgroundResId)
            textView.setTextColor(ContextCompat.getColor(this, if (isSelected) R.color.cool_blue else R.color.black))
            imageView.setColorFilter(ContextCompat.getColor(this, if (isSelected) R.color.cool_blue else R.color.black))
        }

        selectOption(layoutSortTitle, listOf(ZtoAOption), tvSortTitleIcon, tvSortTitle)

        btn_done.setOnClickListener {

        }


        // Set up click listeners for sorting options
        layoutSortTitle.setOnClickListener {

            selectOption(layoutSortTitle, listOf(ZtoAOption), tvSortTitleIcon, tvSortTitle)
        }

        layoutSortDuration.setOnClickListener {
            selectOption(layoutSortDuration, listOf(lengthOption), tvSortLengthIcon, tvSortLength)
        }

        layoutSortDateAdded.setOnClickListener {
            selectOption(layoutSortDateAdded, listOf(dateOption), tvSortDateIcon, tvSortDate)
        }

        layoutSortSize.setOnClickListener {
            selectOption(layoutSortSize, listOf(sizeOption), tvSortSizeIcon, tvSortSize)
        }

        // Set up click listeners for individual options within sorting
        tv_a_to_zLayout.setOnClickListener {
            handleIndividualSelection(tv_a_to_zLayout, option1TextView, option1ImageView, isLeftSide = true, isSelected = true)
        }

        tv_z_to_aLayout.setOnClickListener {
            handleIndividualSelection(tv_z_to_aLayout, option2TextView, option2ImageView, isLeftSide = false, isSelected = true)
        }

        shortestLayout.setOnClickListener {
            handleIndividualSelection(shortestLayout, option3TextView, option3ImageView, isLeftSide = true, isSelected = true)
        }

        LongestLayout.setOnClickListener {
            handleIndividualSelection(LongestLayout, option4TextView, option4ImageView, isLeftSide = false, isSelected = true)
        }

        OldestLayout.setOnClickListener {
            handleIndividualSelection(OldestLayout, option5TextView, option5ImageView, isLeftSide = true, isSelected = true)
        }

        newestLayout.setOnClickListener {
            handleIndividualSelection(newestLayout, option6TextView, option6ImageView, isLeftSide = false, isSelected = true)
        }

        SmallestLayout.setOnClickListener {
            handleIndividualSelection(SmallestLayout, option7TextView, option7ImageView, isLeftSide = true, isSelected = true)
        }

        LargestLayout.setOnClickListener {
            handleIndividualSelection(LargestLayout, option8TextView, option8ImageView, isLeftSide = false, isSelected = true)
        }

        // Show the AlertDialog
        dialog.show()
    }




    private fun toggleRepeatMode() {
        isClick = !isClick // Toggle the repeat mode state
        if (isClick) {
            // Set the color to cool blue
            binding.chipIcon1.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            binding.isClick.background = ContextCompat.getDrawable(this, R.drawable.transparent_cool_blue)

        } else {
            // Clear the color filter to reset to default color
            binding.chipIcon1.clearColorFilter()
            binding.isClick.background = ContextCompat.getDrawable(this, R.drawable.curver_music)

        }
    }
    private fun toggleShuffleMode() {
        isClickShuffle = !isClickShuffle // Toggle the repeat mode state
        if (isClickShuffle) {
            // Set the color to cool blue
            binding.chipIcon2.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            binding.shuffle.background = ContextCompat.getDrawable(this, R.drawable.transparent_cool_blue)

        } else {
            // Clear the color filter to reset to default color
            binding.chipIcon2.clearColorFilter()
            binding.shuffle.background = ContextCompat.getDrawable(this, R.drawable.curver_music)

        }
    }





    private fun setupRecyclerView() {
        videoAdapter = PlaylistVideoShowAdapter(this, videoList) { video ->
            lifecycleScope.launch {
                removeVideoFromPlaylist(video)
                loadVideosForPlaylist()
            }
        }
        videoAdapter.selectionChangeListener = this // Set the listener
        binding.videoOfPlaylistRV.layoutManager = LinearLayoutManager(this)
        binding.videoOfPlaylistRV.adapter = videoAdapter
    }
    private suspend fun removeVideoFromPlaylist(video: VideoData) {
        withContext(Dispatchers.IO) {
            // Remove the video from the playlist
            db.playlistDao().deleteVideoFromPlaylist(playlistId, video.id)
        }
        // Reload the videos after removing
        loadVideosForPlaylist()
        // Reload the videos and update UI after removing
        loadVideosFromDatabase()
        val intent = Intent("UPDATE_PLAYLIST_FOLDER")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    private suspend fun loadVideosForPlaylist() {
        val playlistWithVideos = withContext(Dispatchers.IO) { db.playlistDao().getPlaylistWithVideos(playlistId) }
        videoList.clear()
        videoList.addAll(playlistWithVideos.videos.map {
            VideoData(
                id = it.id,
                title = it.title,
                duration = it.duration,
                folderName = it.folderName,
                size = it.size,
                path = it.path,
                artUri = Uri.parse(it.artUri),
                dateAdded = it.dateAdded,
                isNew = it.isNew,
                isPlayed = it.isPlayed
            )
        })
        videoAdapter.updateVideoList(videoList)
        checkIfRecyclerViewIsEmpty()
    }


    fun addSelectedVideos(selectedVideos: List<VideoData>) {
        lifecycleScope.launch(Dispatchers.IO) {  // Use Dispatchers.IO to run on a background thread
            selectedVideos.forEach { video ->
                // Check if the video already exists in the videos table
                val videoExists = db.playlistDao().videoExists(video.id)
                if (!videoExists) {
                    // Insert the video only if it doesn't already exist
                    db.playlistDao().insertVideo(VideoEntity(
                        id = video.id,
                        title = video.title,
                        duration = video.duration,
                        folderName = video.folderName,
                        size = video.size,
                        path = video.path,
                        artUri = video.artUri.toString(),
                        dateAdded = video.dateAdded,
                        isNew = video.isNew,
                        isPlayed = video.isPlayed
                    ))
                }

                // Check if the video is already in the playlist
                val isVideoInPlaylist = db.playlistDao().isVideoInPlaylist(playlistId, video.id)
                if (!isVideoInPlaylist) {
                    // Insert the video into the playlist if it's not already in the playlist
                    db.playlistDao().insertPlaylistVideoCrossRef(PlaylistVideoCrossRef(playlistId, video.id))
                } else {
                    // Show a toast if the video is already in the playlist
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlaylistVideoActivity, "${video.title} is already in the playlist", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                loadVideosFromDatabase()
                loadVideosForPlaylist()
                // Send a broadcast to notify the PlaylistFolderActivity
                val intent = Intent("UPDATE_PLAYLIST_FOLDER")
                LocalBroadcastManager.getInstance(this@PlaylistVideoActivity).sendBroadcast(intent)
            }
        }
    }



    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (videoAdapter.isSelectionMode) {
            videoAdapter.disableSelectionMode() // Method to be added in the adapter
        } else  {
            // Finish the activity as usual
            super.onBackPressed()
            overridePendingTransition(R.anim.from_left, R.anim.to_right)
        }
    }




    private fun checkIfRecyclerViewIsEmpty() {
        if (videoAdapter.itemCount == 0) {
            // Hide menu and layout elements when the playlist is empty
            updateToolbarMenu(false)
            binding.linearLayout27.visibility = View.GONE
            binding.AddVideoLayout.visibility = View.VISIBLE
        } else {
            // Show menu and layout elements when the playlist is not empty
            updateToolbarMenu(true)
            binding.linearLayout27.visibility = View.VISIBLE
            binding.AddVideoLayout.visibility = View.GONE
        }
    }

    private fun updateToolbarMenu(visible: Boolean) {
        if (visible) {
            // Inflate the menu if it needs to be visible
            binding.playlistToolbar.menu.clear() // Clear any existing menu items
            binding.playlistToolbar.inflateMenu(R.menu.visible_playlist_menu)
        } else {
            // Clear the menu if it should be hidden
            binding.playlistToolbar.menu.clear()
        }
    }

    private fun loadVideosFromDatabase() {
        lifecycleScope.launch {
            // Fetch playlist with videos
            val playlistWithVideos = db.playlistDao().getPlaylistWithVideos(playlistId)

            // Fetch playlist details
            val playlistName = playlistWithVideos.playlist.name
            val videoCount = db.playlistDao().getVideoCountForPlaylist(playlistId)
            val totalDurationMillis = db.playlistDao().getTotalDurationForPlaylist(playlistId)
            val totalDuration = formatDuration(totalDurationMillis)

            // Fetch the URI of the first video's image
            val firstVideoImageUri = db.playlistDao().getFirstVideoImageUri(playlistId)

            // Update UI
            binding.playlistName.text = playlistName

            // Check if there are videos in the playlist
            val videoCountText = if (videoCount == 0) {
                "$videoCount videos"
            } else {
                "$videoCount videos • $totalDuration"
            }
            binding.videoCount.text = videoCountText

// Set the first video's image if available
            if (firstVideoImageUri != null) {
                Glide.with(this@PlaylistVideoActivity)
                    .load(firstVideoImageUri)
                    .placeholder(R.color.placeholder_image) // Replace with a placeholder if desired
                    .into(binding.playlistFirstVideoImage)
            } else {
                // Optionally, you can set a default image or hide the ImageView if no image is available
                binding.playlistFirstVideoImage.setImageResource(R.color.placeholder_image) // Replace with a default image
            }

            // Map videos to VideoData and update the adapter
            val videoDataList = playlistWithVideos.videos.map { videoEntity ->
                VideoData(
                    id = videoEntity.id,
                    title = videoEntity.title,
                    duration = videoEntity.duration,
                    folderName = videoEntity.folderName,
                    size = videoEntity.size,
                    path = videoEntity.path,
                    artUri = Uri.parse(videoEntity.artUri),
                    dateAdded = videoEntity.dateAdded,
                    isNew = videoEntity.isNew,
                    isPlayed = videoEntity.isPlayed
                )
            }
            videoAdapter.updateVideoList(videoDataList)
            checkIfRecyclerViewIsEmpty()
            // Update the playlist name with the current selection count
            val selectedCount = videoAdapter.getSelectedVideos().size
            updatePlaylistName(selectedCount)
        }
    }
    // Method to update the playlist name based on the selected items
    fun updatePlaylistName(selectedCount: Int = 0) {
        lifecycleScope.launch {
            val playlistWithVideos = db.playlistDao().getPlaylistWithVideos(playlistId)

            // Fetch playlist details
            val playlistName = playlistWithVideos.playlist.name
            val title = when {
                isActionModeEnabled && selectedCount == 0 -> "0 videos selected"
                isActionModeEnabled -> "$selectedCount ${if (selectedCount == 1) "video" else "videos"} selected"
                else -> playlistName
            }

            binding.playlistName.text = title
        }
    }

    override fun onSelectionChanged(isAllSelected: Boolean) {
        // Update the `selectAll` button based on selection state
        if (isAllSelected) {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_box_24, 0, 0, 0)
        } else {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.round_crop_square_24, 0, 0, 0)
        }

        if (isActionModeEnabled) {
            if (videoAdapter.getVideos().isNotEmpty()) {

                showBottomToolbar()
            } else {

                hideBottomToolbar()
            }
        }

    }
    private fun showBottomToolbar() {
        binding.bottomToolbarF.visibility = View.VISIBLE
    }

    private fun hideBottomToolbar() {
        binding.bottomToolbarF.visibility = View.GONE
    }




    // Method to update UI based on selection mode
    fun updateSelectionMode(isInSelectionMode: Boolean) {
        isActionModeEnabled = isInSelectionMode
        if (isInSelectionMode) {
            binding.isClick.visibility = View.GONE
            binding.shuffle.visibility = View.GONE
            binding.playandshuffleBtn.visibility = View.GONE
            binding.videoCount.visibility = View.GONE
            binding.selectAll.visibility = View.VISIBLE
            showBottomToolbar()

        } else {
            binding.isClick.visibility = View.VISIBLE
            binding.shuffle.visibility = View.VISIBLE
            binding.playandshuffleBtn.visibility = View.VISIBLE
            binding.videoCount.visibility = View.VISIBLE
            binding.selectAll.visibility = View.GONE
            hideBottomToolbar()

        }
        videoAdapter.notifyDataSetChanged()

    }

    // Format duration in minutes and seconds
    private fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60)) % 24

        return buildString {
            if (hours > 0) {
                append("$hours hrs")
                if (minutes > 0) append(", $minutes mins")
            } else if (minutes > 0) {
                append("$minutes mins")
                if (seconds > 0) append(", $seconds secs")
            } else {
                append("$seconds secs")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatePlaylistReceiver)

    }

}
