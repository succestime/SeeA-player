
package com.jaidev.seeaplayer.MusicPlaylistFunctionality

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.VideoPlaylistFunctionality.PlaylistVideoActivity
import com.jaidev.seeaplayer.allAdapters.MusicPlaylistAdapter.PlaylistMusicShowAdapter
import com.jaidev.seeaplayer.dataClass.MusicPlaylistData.DatabaseClientMusic
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicPlaylistData.MusicEntity
import com.jaidev.seeaplayer.dataClass.MusicPlaylistData.PlaylistMusicCrossRef
import com.jaidev.seeaplayer.databinding.ActivityPlatylistDetailsBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlaylistDetails : AppCompatActivity(), PlaylistMusicShowAdapter.OnSelectionChangeListener {
    lateinit var binding: ActivityPlatylistDetailsBinding

    lateinit var videoAdapter: PlaylistMusicShowAdapter
    private val db by lazy { DatabaseClientMusic.getInstance(this) }
    companion object{
        val videoList = ArrayList<Music>()
        var playlistId: Long = -1

    }
    private var isClickShuffle: Boolean = false // Flag for repeat mode
    private var isAllSelected = false
    private var isActionModeEnabled = false
    private var selectedSortType: PlaylistVideoActivity.SortType? = null

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
        binding = ActivityPlatylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        playlistId = intent.getLongExtra("playlistId", -1)
        if (playlistId == -1L) finish() // Invalid playlist ID

        LocalBroadcastManager.getInstance(this).registerReceiver(updatePlaylistReceiver, IntentFilter("UPDATE_PLAYLIST_MUSIC"))

        setupRecyclerView()
        loadVideosFromDatabase()

        videoAdapter.selectionChangeListener = this // Set the listener for selection changes

        // Initialize the buttons in the disabled state
        updateButtonStates()

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
            val bottomSheet = AddMusicBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
        setSwipeRefreshBackgroundColor()



        binding.shuffle.setOnClickListener {
            toggleShuffleMode()
        }

        binding.playandshuffleBtn.setOnClickListener {
            if (videoList.isNotEmpty()) {
                if (isClickShuffle) {
                    videoList.shuffle() // Shuffle the playlist
                }

                // Start PlayerMusicActivity and pass the list of music
                val intent = Intent(this, PlayerMusicActivity::class.java)
                intent.putParcelableArrayListExtra("SelectedMusicList", ArrayList(videoList))
                startActivity(intent)
                overridePendingTransition(
                    R.anim.slide_in_bottom,
                    R.anim.anim_no_change // Using a transparent animation for exit
                )
            } else {
                Toast.makeText(this, "No music in the playlist to play.", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize the video adapter and set the listener


        binding.selectAll.setOnClickListener {
            isAllSelected = !isAllSelected
            videoAdapter.selectAllVideos(isAllSelected)
            updateSelectAllIcon(isAllSelected)
        }



        binding.playVideo.setOnClickListener {
            val selectedVideos = videoAdapter.getSelectedVideos() // Get the selected videos

            if (selectedVideos.isNotEmpty()) {
                val intent = Intent(this, PlayerMusicActivity::class.java)
                intent.putParcelableArrayListExtra("SelectedMusicList", ArrayList(selectedVideos))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No music selected to play.", Toast.LENGTH_SHORT).show()
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
                                db.playlistMusicDao().deleteMusicFromPlaylist(playlistId, video.musicid)
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
                            val intent = Intent("UPDATE_PLAYLIST_MUSIC")
                            LocalBroadcastManager.getInstance(this@PlaylistDetails).sendBroadcast(intent)
                        }
                    }
                } else {
                    Toast.makeText(this, "No videos selected to remove", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }



    }
    private suspend fun removeDeletedSongsFromPlaylist(deletedMusicPath: String) {
        withContext(Dispatchers.IO) {
            val deletedMusic = db.playlistMusicDao().getMusicByPath(deletedMusicPath)
            if (deletedMusic != null) {
                // Remove the music from the playlist
                db.playlistMusicDao().deleteMusicFromPlaylist(playlistId, deletedMusic.musicid)

                // Remove the music from the database
                db.playlistMusicDao().deleteMusic(deletedMusic.musicid)
            }
        }
        withContext(Dispatchers.Main) {
            loadVideosFromDatabase()
            loadVideosForPlaylist()

            val intent = Intent("UPDATE_PLAYLIST_MUSIC")
            LocalBroadcastManager.getInstance(this@PlaylistDetails).sendBroadcast(intent)
        }
    }
    private suspend fun checkForDeletedSongs() {
        withContext(Dispatchers.IO) {
            val allMusic = db.playlistMusicDao().getAllMusic()
            for (music in allMusic) {
                if (!File(music.path).exists()) {
                    removeDeletedSongsFromPlaylist(music.path)
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun loadVideosFromDatabase() {
        binding.progressBar.visibility = View.VISIBLE // Show ProgressBar

        lifecycleScope.launch {
            checkForDeletedSongs()  // Check for any deleted songs

            val sortOrder = db.playlistMusicDao().getSortOrder(playlistId)

            // Fetch playlist with videos, sorted by the saved sort order
            val sortedVideos = withContext(Dispatchers.IO) {
                when (sortOrder) {
                    PlaylistVideoActivity.SortType.TITLE_ASC -> db.playlistMusicDao().getMusicSortedByTitleAsc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.TITLE_DESC -> db.playlistMusicDao().getMusicSortedByTitleDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DURATION_ASC -> db.playlistMusicDao().getMusicSortedByDurationAsc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DURATION_DESC -> db.playlistMusicDao().getMusicSortedByDurationDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DATE_NEWEST -> db.playlistMusicDao().getMusicSortedByNewest(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DATE_OLDEST -> db.playlistMusicDao().getMusicSortedByOldest(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.SIZE_LARGEST -> db.playlistMusicDao().getMusicSortedBySizeDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.SIZE_SMALLEST -> db.playlistMusicDao().getMusicSortedBySizeAsc(
                        playlistId
                    )
                }
            }

            // Fetch playlist details
            val playlistWithVideos = db.playlistMusicDao().getPlaylistWithMusic(playlistId)
            val playlistName = playlistWithVideos.playlistMusic.name
            val videoCount = db.playlistMusicDao().getMusicCountForPlaylist(playlistId)
            val totalDurationMillis = db.playlistMusicDao().getTotalDurationForPlaylist(playlistId)
            val totalDuration = formatDuration(totalDurationMillis)
            val firstVideoImageUri = db.playlistMusicDao().getFirstMusicImageUri(playlistId, sortOrder)

            binding.playlistName.text = playlistName

            val videoCountText = if (videoCount == 0) {
                "$videoCount musics"
            } else {
                "$videoCount musics • $totalDuration"
            }
            binding.videoCount.text = videoCountText

            // Set the first video's image if available
            if (firstVideoImageUri != null) {
                Glide.with(this@PlaylistDetails)
                    .load(firstVideoImageUri)
                    .placeholder(R.color.placeholder_image)
                    .into(binding.playlistFirstVideoImage)
            } else {
                binding.playlistFirstVideoImage.setImageResource(R.color.placeholder_image) // Replace with a default image
            }
            binding.playlistFirstVideoImage.foreground = ContextCompat.getDrawable(this@PlaylistDetails, R.drawable.gray_overlay)

            videoList.clear()
            videoList.addAll(sortedVideos.map { videoEntity ->
                Music(
                    musicid = videoEntity.musicid,
                    title = videoEntity.title,
                    duration = videoEntity.duration,
                    size = videoEntity.size,
                    path = videoEntity.path,
                    artUri = videoEntity.artUri,
                    dateAdded = videoEntity.dateAdded,
                    album = videoEntity.album,
                    artist = videoEntity.artist
                )
            })
            videoAdapter.updateVideoList(videoList)
            videoAdapter.notifyDataSetChanged()

            binding.progressBar.visibility = View.GONE // Hide ProgressBar

            if (sortedVideos.isEmpty()) {
                checkIfRecyclerViewIsEmpty()
                return@launch // Exit early if the playlist is empty
            }

            // Update the playlist name with the current selection count
            val selectedCount = videoAdapter.getSelectedVideos().size
            updatePlaylistName(selectedCount)
            checkIfRecyclerViewIsEmpty()
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
        updateSelectAllIcon(isAllSelected)
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val selectedVideos = videoAdapter.getSelectedVideos()
        val hasSelection = selectedVideos.isNotEmpty()

        // Update playVideo button state
        binding.playVideo.apply {
            isEnabled = hasSelection
            isClickable = hasSelection
            alpha = if (hasSelection) 1.0f else 0.5f // Change alpha to indicate disabled state
        }

        // Update removeVideo button state
        binding.removeVideo.apply {
            isEnabled = hasSelection
            isClickable = hasSelection
            alpha = if (hasSelection) 1.0f else 0.5f // Change alpha to indicate disabled state
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

        val ZtoAOption = dialogView.findViewById<LinearLayout>(R.id.titleOption)
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
        ZtoAOption.visibility = View.GONE
        lengthOption.visibility = View.GONE
        dateOption.visibility = View.GONE
        sizeOption.visibility = View.GONE

        // Get the default text color from the theme (android:attr/textColorPrimary)
        val typedValue = TypedValue()
        val theme = this.theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val defaultTextColor = ContextCompat.getColor(this, typedValue.resourceId)

        // Function to clear selection
        fun clearSelection(excludeLayout: LinearLayout? = null) {
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

        fun handleIndividualSelection(layout: LinearLayout, textView: TextView, imageView: ImageView, isLeftSide: Boolean, isSelected: Boolean, sortType: PlaylistVideoActivity.SortType) {
            if (isSelected) {
                val color = ContextCompat.getColor(this, R.color.cool_blue)
                val selectedResId = if (isLeftSide) R.drawable.combined_background_left_cool_blue else R.drawable.combined_background_right_cool_blue
                layout.setBackgroundResource(selectedResId)
                clearSelection(layout)
                textView.setTextColor(color)
                imageView.setColorFilter(color)
                selectedSortType = sortType
            }
        }

        // Set click listeners for the sort options
        layoutSortTitle.setOnClickListener {
            selectOption(layoutSortTitle, listOf(ZtoAOption), tvSortTitleIcon, tvSortTitle)
            handleIndividualSelection(tv_a_to_zLayout, option1TextView, option1ImageView, isLeftSide = true, isSelected = true, sortType = PlaylistVideoActivity.SortType.TITLE_ASC)
        }

        layoutSortDuration.setOnClickListener {
            selectOption(layoutSortDuration, listOf(lengthOption), tvSortLengthIcon, tvSortLength)
            handleIndividualSelection(LongestLayout, option4TextView, option4ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.DURATION_DESC)
        }

        layoutSortDateAdded.setOnClickListener {
            selectOption(layoutSortDateAdded, listOf(dateOption), tvSortDateIcon, tvSortDate)
            handleIndividualSelection(newestLayout, option6TextView, option6ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.DATE_NEWEST)
        }

        layoutSortSize.setOnClickListener {
            selectOption(layoutSortSize, listOf(sizeOption), tvSortSizeIcon, tvSortSize)
            handleIndividualSelection(LargestLayout, option8TextView, option8ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.SIZE_LARGEST)
        }

        tv_a_to_zLayout.setOnClickListener {
            handleIndividualSelection(tv_a_to_zLayout, option1TextView, option1ImageView, isLeftSide = true, isSelected = true, sortType = PlaylistVideoActivity.SortType.TITLE_ASC)
            handleIndividualSelection(tv_z_to_aLayout, option2TextView, option2ImageView, isLeftSide = false, isSelected = false, sortType = PlaylistVideoActivity.SortType.TITLE_DESC)
        }

        tv_z_to_aLayout.setOnClickListener {
            handleIndividualSelection(tv_z_to_aLayout, option2TextView, option2ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.TITLE_DESC)
            handleIndividualSelection(tv_a_to_zLayout, option1TextView, option1ImageView, isLeftSide = true, isSelected = false, sortType = PlaylistVideoActivity.SortType.TITLE_ASC)
        }

        shortestLayout.setOnClickListener {
            handleIndividualSelection(shortestLayout, option3TextView, option3ImageView, isLeftSide = true, isSelected = true, sortType = PlaylistVideoActivity.SortType.DURATION_ASC)
            handleIndividualSelection(LongestLayout, option4TextView, option4ImageView, isLeftSide = false, isSelected = false, sortType = PlaylistVideoActivity.SortType.DURATION_DESC)
        }

        LongestLayout.setOnClickListener {
            handleIndividualSelection(LongestLayout, option4TextView, option4ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.DURATION_DESC)
            handleIndividualSelection(shortestLayout, option3TextView, option3ImageView, isLeftSide = true, isSelected = false, sortType = PlaylistVideoActivity.SortType.DURATION_ASC)
        }

        OldestLayout.setOnClickListener {
            handleIndividualSelection(OldestLayout, option5TextView, option5ImageView, isLeftSide = true, isSelected = true, sortType = PlaylistVideoActivity.SortType.DATE_OLDEST)
            handleIndividualSelection(newestLayout, option6TextView, option6ImageView, isLeftSide = false, isSelected = false, sortType = PlaylistVideoActivity.SortType.DATE_NEWEST)
        }

        newestLayout.setOnClickListener {
            handleIndividualSelection(newestLayout, option6TextView, option6ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.DATE_NEWEST)
            handleIndividualSelection(OldestLayout, option5TextView, option5ImageView, isLeftSide = true, isSelected = false, sortType = PlaylistVideoActivity.SortType.DATE_OLDEST)
        }

        SmallestLayout.setOnClickListener {
            handleIndividualSelection(SmallestLayout, option7TextView, option7ImageView, isLeftSide = true, isSelected = true, sortType = PlaylistVideoActivity.SortType.SIZE_SMALLEST)
            handleIndividualSelection(LargestLayout, option8TextView, option8ImageView, isLeftSide = false, isSelected = false, sortType = PlaylistVideoActivity.SortType.SIZE_LARGEST)
        }

        LargestLayout.setOnClickListener {
            handleIndividualSelection(LargestLayout, option8TextView, option8ImageView, isLeftSide = false, isSelected = true, sortType = PlaylistVideoActivity.SortType.SIZE_LARGEST)
            handleIndividualSelection(SmallestLayout, option7TextView, option7ImageView, isLeftSide = true, isSelected = false, sortType = PlaylistVideoActivity.SortType.SIZE_SMALLEST)
        }

        cancel_btn.setOnClickListener {
            dialog.dismiss()
        }

        btn_done.setOnClickListener {
            selectedSortType?.let {
                lifecycleScope.launch {
                    saveSortOrder(it)
                    sortVideos(it)
                }
            }
            dialog.dismiss() // Close the dialog
        }

        // Show the dialog
        dialog.show()
        fun updateSortOrderUI(sortType: PlaylistVideoActivity.SortType) {

            when (sortType) {
                PlaylistVideoActivity.SortType.TITLE_ASC, PlaylistVideoActivity.SortType.TITLE_DESC -> {
                    selectOption(layoutSortTitle, listOf(ZtoAOption), tvSortTitleIcon, tvSortTitle)
                    handleIndividualSelection(
                        if (sortType == PlaylistVideoActivity.SortType.TITLE_ASC) tv_a_to_zLayout else tv_z_to_aLayout,
                        if (sortType == PlaylistVideoActivity.SortType.TITLE_ASC) option1TextView else option2TextView,
                        if (sortType == PlaylistVideoActivity.SortType.TITLE_ASC) option1ImageView else option2ImageView,
                        isLeftSide = sortType == PlaylistVideoActivity.SortType.TITLE_ASC,
                        isSelected = true,
                        sortType = sortType
                    )
                }
                PlaylistVideoActivity.SortType.DURATION_ASC, PlaylistVideoActivity.SortType.DURATION_DESC -> {
                    selectOption(layoutSortDuration, listOf(lengthOption), tvSortLengthIcon, tvSortLength)
                    handleIndividualSelection(
                        if (sortType == PlaylistVideoActivity.SortType.DURATION_ASC) shortestLayout else LongestLayout,
                        if (sortType == PlaylistVideoActivity.SortType.DURATION_ASC) option3TextView else option4TextView,
                        if (sortType == PlaylistVideoActivity.SortType.DURATION_ASC) option3ImageView else option4ImageView,
                        isLeftSide = sortType == PlaylistVideoActivity.SortType.DURATION_ASC,
                        isSelected = true,
                        sortType = sortType
                    )
                }
                PlaylistVideoActivity.SortType.DATE_NEWEST, PlaylistVideoActivity.SortType.DATE_OLDEST -> {
                    selectOption(layoutSortDateAdded, listOf(dateOption), tvSortDateIcon, tvSortDate)
                    handleIndividualSelection(
                        if (sortType == PlaylistVideoActivity.SortType.DATE_OLDEST) OldestLayout else newestLayout,
                        if (sortType == PlaylistVideoActivity.SortType.DATE_OLDEST) option5TextView else option6TextView,
                        if (sortType == PlaylistVideoActivity.SortType.DATE_OLDEST) option5ImageView else option6ImageView,
                        isLeftSide = sortType == PlaylistVideoActivity.SortType.DATE_OLDEST,
                        isSelected = true,
                        sortType = sortType
                    )
                }
                PlaylistVideoActivity.SortType.SIZE_SMALLEST, PlaylistVideoActivity.SortType.SIZE_LARGEST -> {
                    selectOption(layoutSortSize, listOf(sizeOption), tvSortSizeIcon, tvSortSize)
                    handleIndividualSelection(
                        if (sortType == PlaylistVideoActivity.SortType.SIZE_SMALLEST) SmallestLayout else LargestLayout,
                        if (sortType == PlaylistVideoActivity.SortType.SIZE_SMALLEST) option7TextView else option8TextView,
                        if (sortType == PlaylistVideoActivity.SortType.SIZE_SMALLEST) option7ImageView else option8ImageView,
                        isLeftSide = sortType == PlaylistVideoActivity.SortType.SIZE_SMALLEST,
                        isSelected = true,
                        sortType = sortType
                    )
                }
            }
        }

        // Initialize the UI state based on the current sort order
        lifecycleScope.launch {
            val currentSortOrder = db.playlistMusicDao().getSortOrder(playlistId)
            updateSortOrderUI(currentSortOrder)
        }
    }


    private fun setupRecyclerView() {
        videoAdapter = PlaylistMusicShowAdapter(this, videoList) { video ->
            lifecycleScope.launch {
                removeVideoFromPlaylist(video)
                loadVideosForPlaylist()
            }
        }
        videoAdapter.selectionChangeListener = this@PlaylistDetails
        binding.videoOfPlaylistRV.layoutManager = LinearLayoutManager(this)
        binding.videoOfPlaylistRV.adapter = videoAdapter
    }

    private suspend fun removeVideoFromPlaylist(video: Music) {
        withContext(Dispatchers.IO) {
            // Remove the video from the playlist
            db.playlistMusicDao().deleteMusicFromPlaylist(playlistId, video.musicid)
        }
        // Reload the videos after removing
        loadVideosForPlaylist()
        // Reload the videos and update UI after removing
        loadVideosFromDatabase()
        val intent = Intent("UPDATE_PLAYLIST_MUSIC")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    private suspend fun loadVideosForPlaylist() {
        val playlistWithVideos = withContext(Dispatchers.IO) {
            db.playlistMusicDao().getPlaylistWithMusic(playlistId)
        }

        videoList.clear()
        videoList.addAll(playlistWithVideos.music.map {
            Music(
                musicid = it.musicid,
                title = it.title,
                duration = it.duration,
                size = it.size,
                path = it.path,
                artUri = it.artUri,
                dateAdded = it.dateAdded,
                album = it.album ,
                artist = it.artist
            )
        })

        videoAdapter.updateVideoList(videoList)
        checkIfRecyclerViewIsEmpty()
        // Fetch the saved sort order
        val sortOrder = db.playlistMusicDao().getSortOrder(playlistId)
        sortVideos(sortOrder)

    }



    private suspend fun saveSortOrder(sortType: PlaylistVideoActivity.SortType) {
        withContext(Dispatchers.IO) {
            db.playlistMusicDao().updateSortOrder(playlistId, sortType)
        }
    }


    private fun sortVideos(sortType: PlaylistVideoActivity.SortType) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.playlistMusicDao().updateSortOrder(playlistId, sortType) // Save the sort order
                val sortedVideos = when (sortType) {
                    PlaylistVideoActivity.SortType.TITLE_ASC -> db.playlistMusicDao().getMusicSortedByTitleAsc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.TITLE_DESC -> db.playlistMusicDao().getMusicSortedByTitleDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DURATION_ASC -> db.playlistMusicDao().getMusicSortedByDurationAsc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DURATION_DESC -> db.playlistMusicDao().getMusicSortedByDurationDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DATE_NEWEST -> db.playlistMusicDao().getMusicSortedByNewest(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.DATE_OLDEST -> db.playlistMusicDao().getMusicSortedByOldest(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.SIZE_LARGEST -> db.playlistMusicDao().getMusicSortedBySizeDesc(
                        playlistId
                    )
                    PlaylistVideoActivity.SortType.SIZE_SMALLEST -> db.playlistMusicDao().getMusicSortedBySizeAsc(
                        playlistId
                    )
                }
                videoList.clear()
                videoList.addAll(sortedVideos.map {
                    Music(
                        musicid = it.musicid,
                        title = it.title,
                        duration = it.duration,
                        size = it.size,
                        path = it.path,
                        artUri = it.artUri,
                        dateAdded = it.dateAdded,
                        album = it.album ,
                        artist = it.artist
                    )
                })
                withContext(Dispatchers.Main) {
                    // Update your UI here
                    videoAdapter.updateVideoList(videoList)
                    loadVideosFromDatabase()
                    // Send a broadcast to notify the PlaylistFolderActivity
                    val intent = Intent("UPDATE_PLAYLIST_MUSIC")
                    LocalBroadcastManager.getInstance(this@PlaylistDetails).sendBroadcast(intent)
                }

            }
        }
    }

    fun addSelectedVideos(selectedVideos: List<Music>) {
        lifecycleScope.launch(Dispatchers.IO) {  // Use Dispatchers.IO to run on a background thread
            selectedVideos.forEach { music ->
                // Check if the video already exists in the videos table
                val videoExists = db.playlistMusicDao().musicExists(music.musicid)
                if (!videoExists) {
                    // Insert the video only if it doesn't already exist
                    db.playlistMusicDao().insertMusic(
                        MusicEntity(
                            musicid = music.musicid,
                            title = music.title,
                            duration = music.duration,
                            size = music.size,
                            path = music.path,
                            artUri = music.artUri,
                            dateAdded = music.dateAdded,
                            album = music.album ,
                            artist = music.artist
                        )
                    )
                }

                // Check if the video is already in the playlist
                val isVideoInPlaylist = db.playlistMusicDao().isMusicInPlaylist(playlistId, music.musicid)
                if (!isVideoInPlaylist) {
                    // Insert the video into the playlist if it's not already in the playlist
                    db.playlistMusicDao().insertPlaylistMusicCrossRef(
                        PlaylistMusicCrossRef(
                        playlistId, music.musicid)
                    )
                } else {
                    // Show a toast if the video is already in the playlist
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlaylistDetails, "${music.title} is already in the playlist", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                loadVideosFromDatabase()
                loadVideosForPlaylist()
                // Send a broadcast to notify the PlaylistFolderActivity
                val intent = Intent("UPDATE_PLAYLIST_MUSIC")
                LocalBroadcastManager.getInstance(this@PlaylistDetails).sendBroadcast(intent)
            }
        }
    }


    fun updatePlaylistName(selectedCount: Int = 0) {
        lifecycleScope.launch {
            val playlistWithMusic = db.playlistMusicDao().getPlaylistWithMusic(playlistId)

            // Fetch playlist details
            val playlistName = playlistWithMusic.playlistMusic.name
            val title = when {
                isActionModeEnabled && selectedCount == 0 -> "0 musics selected"
                isActionModeEnabled -> "$selectedCount ${if (selectedCount == 1) "music" else "musics"} selected"
                else -> playlistName
            }

            binding.playlistName.text = title
        }
    }


    fun showBottomToolbar() {
        binding.bottomToolbarF.visibility = View.VISIBLE
    }

    private fun hideBottomToolbar() {
        binding.bottomToolbarF.visibility = View.GONE
    }




    // Method to update UI based on selection mode
    @SuppressLint("NotifyDataSetChanged")
    fun updateSelectionMode(isInSelectionMode: Boolean) {
        isActionModeEnabled = isInSelectionMode
        if (isInSelectionMode) {

            binding.shuffle.visibility = View.GONE
            binding.playandshuffleBtn.visibility = View.GONE
            binding.videoCount.visibility = View.GONE
            binding.selectAll.visibility = View.VISIBLE
            showBottomToolbar()

        } else {

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


    private fun checkIfRecyclerViewIsEmpty() {
        if (videoAdapter.itemCount == 0) {
            // Hide menu and layout elements when the playlist is empty
            updateToolbarMenu(false)
            binding.linearLayout27.visibility = View.GONE
            binding.playlistFirstVideoImage.visibility = View.INVISIBLE
            binding.AddVideoLayout.visibility = View.VISIBLE
        } else {
            // Show menu and layout elements when the playlist is not empty
            updateToolbarMenu(true)
            binding.linearLayout27.visibility = View.VISIBLE
            binding.playlistFirstVideoImage.visibility = View.VISIBLE

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
    private fun updateSelectAllIcon(isSelected: Boolean) {
        if (isSelected) {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.check_box_24, 0, 0, 0)
        } else {
            binding.selectAll.setCompoundDrawablesWithIntrinsicBounds(R.drawable.round_crop_square_24, 0, 0, 0)
        }
    }



    private fun handleFirstMenuItemClick() {
        val playlistId = intent.getLongExtra("playlistId", -1)
        val bottomSheetFragment = MorePlaylistMusicBottomSheetFragment.newInstance(playlistId)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
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
}
