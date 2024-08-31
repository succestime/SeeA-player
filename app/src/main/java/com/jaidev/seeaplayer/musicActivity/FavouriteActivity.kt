
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.jaidev.seeaplayer.AddMusicFavBottomSheetFragment
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.AddToPlaylistViewAdapter
import com.jaidev.seeaplayer.allAdapters.FavouriteAdapter
import com.jaidev.seeaplayer.dataClass.DatabaseClientMusic
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.MusicFavDatabase
import com.jaidev.seeaplayer.dataClass.MusicFavEntity
import com.jaidev.seeaplayer.dataClass.PlaylistMusic
import com.jaidev.seeaplayer.dataClass.PlaylistMusicCrossRef
import com.jaidev.seeaplayer.dataClass.PlaylistMusicEntity
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityFavouriteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FavouriteActivity : AppCompatActivity(), FavouriteAdapter.OnSelectionModeChangeListener
{

    private lateinit var binding: ActivityFavouriteBinding
    private lateinit var adapter: FavouriteAdapter
    private lateinit var musicDatabase: MusicFavDatabase
    private var isAllSelected = false

    companion object {
        var favouritesChanged: Boolean = false
        var favouriteSongs: ArrayList<Music> = ArrayList()
    }
    private val favouritesUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadFavouriteSongs() // Refresh the list when a broadcast is received
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this, theme)
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        musicDatabase = MusicFavDatabase.getDatabase(this)
        binding.favouriteRV.setHasFixedSize(true)
        binding.favouriteRV.setItemViewCacheSize(50)
        binding.favouriteRV.layoutManager =  LinearLayoutManager(this)
        adapter = FavouriteAdapter(this, favouriteSongs , this)
        binding.favouriteRV.adapter = adapter

        favouritesChanged = false

        // Set the default navigation icon to exit the activity
        binding.playlistToolbar.setNavigationOnClickListener {
            finish()
        }
        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            favouritesUpdatedReceiver, IntentFilter("com.yourpackage.FAVORITES_UPDATED")
        )
        binding.addVideosButton.setOnClickListener {
            val bottomSheetFragment = AddMusicFavBottomSheetFragment.newInstance(favouriteSongs)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
        checkAndRemoveDeletedSongs()
        setSwipeRefreshBackgroundColor()
        loadFavouriteSongs()
    }
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(
            R.layout.favourite_music_more_bottom_sheet, // Replace with your actual layout file name
            null
        )
        bottomSheetDialog.setContentView(bottomSheetView)

        // Set up the views in the bottom sheet if needed
        val addSongInFavouritesButton =
            bottomSheetView.findViewById<LinearLayout>(R.id.addSongInFavouritesButton)
//        val addToHomeScreenButton =
//            bottomSheetView.findViewById<LinearLayout>(R.id.addToHomeScreenButton)
        val multipleItemSelectButton =
            bottomSheetView.findViewById<LinearLayout>(R.id.multipleItemSelectButton)
        val shareButton = bottomSheetView.findViewById<LinearLayout>(R.id.shareButton)
        val removeAllFavSongButton = bottomSheetView.findViewById<LinearLayout>(R.id.removeAllButton)
        val imageThumbnail = bottomSheetView.findViewById<ShapeableImageView>(R.id.imageThumbnail)
        val textSubtitle = bottomSheetView.findViewById<TextView>(R.id.textSubtitle)


        // Set the image thumbnail and text subtitle
        if (favouriteSongs.isNotEmpty()) {
            val firstSong = favouriteSongs[0]

            Glide.with(this)
                .load(firstSong.artUri)
                .apply(
                    RequestOptions()
                        .placeholder(R.color.gray) // Use the newly created drawable
                        .error(R.drawable.music_note_svgrepo_com) // Use the newly created drawable
                        .centerCrop()
                )
                .into(imageThumbnail)
            // Set the subtitle text with the total number of favorite songs
            textSubtitle.text = "${favouriteSongs.size} Songs"
        } else {
            // If no songs are available, set a default image and text
            imageThumbnail.setImageResource(R.drawable.music_note_svgrepo_com)
            textSubtitle.text = "No Songs"
        }
        addSongInFavouritesButton.setOnClickListener {
            val bottomSheetFragment = AddMusicFavBottomSheetFragment.newInstance(favouriteSongs)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            bottomSheetDialog.dismiss()
        }



//        // Your existing onClickListener setup
//        addToHomeScreenButton.setOnClickListener {
////            requestPinShortcutPermission()
//            bottomSheetDialog.dismiss()
//        }
        multipleItemSelectButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Enable selection mode in the adapter
            adapter.enableSelectionMode()
            // Call onSelectionModeChanged to update the UI accordingly
            onSelectionModeChanged(true, 0, favouriteSongs.size)
        }

        removeAllFavSongButton.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            val dialogView = LayoutInflater.from(this).inflate(R.layout.favurite_remove_all, null)
            builder.setView(dialogView)
                .setCancelable(false)

            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeAllMusicButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeAllMusicButton.setOnClickListener {
                lifecycleScope.launch {
                    // Delete all songs from the database
                    musicDatabase.musicFavDao().deleteAllMusic()

                    // Clear the list and notify the adapter
                    favouriteSongs.clear()
                    adapter.notifyDataSetChanged()

                    // Update UI based on the empty state
                    shuffleEmpty()

                    // Dismiss the dialog after deletion
                    dialog.dismiss()

                }
            }

            dialog.show()
            bottomSheetDialog.dismiss()
        }

        shareButton.setOnClickListener {
            shareFavoriteSongs()
            bottomSheetDialog.dismiss()
        }

        // Show the bottom sheet dialog
        bottomSheetDialog.show()
    }

//    @SuppressLint("ObsoleteSdkInt")
//    private fun requestPinShortcutPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val shortcutManager = getSystemService(ShortcutManager::class.java)
//            if (!shortcutManager.isRequestPinShortcutSupported) {
//                Toast.makeText(this, "Pinning shortcuts is not supported on your device.", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // Check if the shortcut already exists
//            val existingShortcuts = shortcutManager.pinnedShortcuts
//            val shortcutExists = existingShortcuts.any { it.id == "favourites_shortcut" }
//
//            if (shortcutExists) {
//                // Shortcut already exists, show a toast and return
//                Toast.makeText(this, "Shortcut already exists on the home screen.", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // Create the intent to open the FavouriteActivity
//            val favouriteIntent = Intent(this, FavouriteActivity::class.java).apply {
//                action = Intent.ACTION_VIEW
//            }
//
//            // Create the intent to open MainActivity
//            val mainIntent = Intent(this, MainActivity::class.java)
//
//            // Create a stack builder to simulate the back stack
//            val stackBuilder = TaskStackBuilder.create(this).apply {
//                addNextIntentWithParentStack(mainIntent)
//                addNextIntent(favouriteIntent)
//            }
//
//            // Get the PendingIntent for the task stack
//            val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
//
//            // Create the shortcut
//            val shortcutInfo = ShortcutInfo.Builder(this, "favourites_shortcut")
//                .setShortLabel("My Favourites")
//                .setLongLabel("My Favourites")
//                .setIcon(Icon.createWithResource(this, R.drawable.playlist_minimalistic_3_svgrepo_com)) // Replace with your actual icon
//                .setIntent(favouriteIntent) // This is for the actual launch of FavouriteActivity
//                .build()
//
//            // Request to pin the shortcut
//            shortcutManager.requestPinShortcut(shortcutInfo, pendingIntent?.intentSender)
//        } else {
//            // Handle for versions below Android O
//            Toast.makeText(this, "Your Android version does not support pinned shortcuts.", Toast.LENGTH_SHORT).show()
//        }
//    }




    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (favouritesChanged) {
            loadFavouriteSongs()
            adapter.updateFavourites(favouriteSongs)
            adapter.notifyDataSetChanged()
            favouritesChanged = false
        }

        checkAndRemoveDeletedSongs()  // Check for and remove deleted songs

    }

    override fun onPause() {
        super.onPause()
        checkAndRemoveDeletedSongs()  // Check for and remove deleted songs

    }
    private fun shareFavoriteSongs() {
        if (favouriteSongs.isNotEmpty()) {
            // Create an Intent to share multiple files
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*" // Set the MIME type for audio files
                putExtra(Intent.EXTRA_SUBJECT, "Sharing Favourite Songs") // Optional
            }

            // Convert the file paths of the favorite songs to URIs
            val uris = ArrayList<Uri>()
            val shareContent = StringBuilder()

            // Create a temporary text file for song details
            val songDetailsFile = File(cacheDir, "song_details.txt")

            songDetailsFile.bufferedWriter().use { writer ->
                for (song in favouriteSongs) {
                    // Append song details to the text content
                    shareContent.append("Title: ${song.title}\n")
                    shareContent.append("Artist: ${song.artist}\n")
                    shareContent.append("Album: ${song.album}\n\n")

                    // Convert file path to URI
                    val file = File(song.path)
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider", // Replace with your package name
                        file
                    )
                    uris.add(uri)
                }

                // Write song details to the file
                writer.write(shareContent.toString())
            }

            // Add the song details text file to the list of URIs
            val detailsUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                songDetailsFile
            )
            uris.add(detailsUri)

            // Add URIs and text content to the Intent
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Start the share activity
            if (uris.isNotEmpty()) {
                startActivity(Intent.createChooser(shareIntent, "Share Songs Via"))
            } else {
                Toast.makeText(this, "No songs to share", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No favorite songs to share", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFavouriteSongs() {
        // Load the favorite songs from the database
        lifecycleScope.launch {
            val favoriteEntities = musicDatabase.musicFavDao().getAllMusic()
            favouriteSongs.clear()
            favouriteSongs.addAll(favoriteEntities.map { it.toMusic() })
            adapter.notifyDataSetChanged()
            // Conditionally set the menu item listener based on the number of favorite songs
            if (favouriteSongs.isNotEmpty()) {
                binding.playlistToolbar.menu.findItem(R.id.MorePlaylist).isVisible = true

                // Set up menu item click listener only if there are favorite songs
                binding.playlistToolbar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.MorePlaylist -> {
                            showBottomSheetDialog()
                            true
                        }
                        else -> false
                    }
                }
            } else {
                // Hide the menu items if there are no favorite songs
                binding.playlistToolbar.menu.findItem(R.id.MorePlaylist).isVisible = false


            }
            shuffleEmpty()  // Update shuffle button and empty state view
            checkAndRemoveDeletedSongs()  // Check for and remove deleted songs

        }
    }


    private fun shuffleEmpty() {
        if (favouriteSongs.size < 1) {
            binding.shuffleBtnFA.visibility = View.INVISIBLE
        } else {
            binding.shuffleBtnFA.visibility = View.VISIBLE
        }

        binding.shuffleBtnFA.setOnClickListener {
            val intent = Intent(this, PlayerMusicActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouriteShuffle")
            startActivity(intent)
        }

        // Update visibility of emptyStateLayout and playlistToolbar.menu
        if (favouriteSongs.isEmpty()) {
            binding.AddVideoLayout.visibility = View.VISIBLE
            binding.playlistToolbar.menu.setGroupVisible(0, false) // Make menu visible
        } else {
            binding.AddVideoLayout.visibility = View.GONE
            binding.playlistToolbar.menu.setGroupVisible(0, true) // Hide menu
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
    @SuppressLint("NotifyDataSetChanged")
    override fun onSelectionModeChanged(isSelectionMode: Boolean, selectedCount: Int, totalCount: Int) {
        if (isSelectionMode) {
            // Update toolbar title
            binding.playlistToolbar.title = "$selectedCount/$totalCount Selected"
            // Show the bottom toolbar
            binding.bottomToolbarF.visibility = View.VISIBLE
            // Clear the current menu and inflate the multi-select menu
            binding.playlistToolbar.menu.clear()
            binding.playlistToolbar.inflateMenu(R.menu.multi_select_menu)

            // Set the appropriate icon based on whether all items are selected
            val checkIcon = if (selectedCount == totalCount) {
                R.drawable.check_box_24 // Icon when all items are selected
            } else {
                R.drawable.round_crop_square_24 // Icon when not all items are selected
            }
            binding.playlistToolbar.menu.findItem(R.id.checkMulti).icon =
                ContextCompat.getDrawable(this, checkIcon)

            val selectedVideos = adapter.getSelectedVideos()
            val hasSelection = selectedVideos.isNotEmpty()

            // Update playVideo button state
            binding.addToPlaylistVideo.apply {
                isEnabled = hasSelection
                isClickable = hasSelection
                alpha = if (hasSelection) 1.0f else 0.5f // Change alpha to indicate disabled state
            }
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

            // Set up the navigation icon and its click listener to exit selection mode
            binding.playlistToolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.browser_clear)
            binding.playlistToolbar.setNavigationOnClickListener {
                exitSelectionMode()
            }

            binding.playVideo.setOnClickListener {
                // Get the selected music items from the adapter
                val selectedPVVideos = adapter.getSelectedVideos()
                // Check if any items are selected
                if (selectedPVVideos.isNotEmpty()) {
                    // Prepare the intent to start PlayerMusicActivity
                    val intent = Intent(this, PlayerMusicActivity::class.java)
                    // Convert the selectedVideos list to an ArrayList of Music
                    val musicArrayList = ArrayList(selectedPVVideos)
                    // Pass the selected music list to PlayerMusicActivity
                    intent.putParcelableArrayListExtra("SelectedMusicList", musicArrayList)
                    // Start PlayerMusicActivity
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No music selected to play", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle add to playlist action
            binding.addToPlaylistVideo.setOnClickListener {
                if (hasSelection) {
                    showAddToPlaylistDialog(selectedVideos)
                }
            }
            binding.removeVideo.setOnClickListener {
                // Inflate the custom layout for the dialog
                val dialogView = layoutInflater.inflate(
                    R.layout.playlist_video_selected_remove,
                    null
                )

                // Create the AlertDialog
                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                // Get references to the TextView and buttons in the custom layout
                val messageTextView = dialogView.findViewById<TextView>(R.id.message)
                val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
                val removeButton = dialogView.findViewById<Button>(R.id.remove_button)

                // Update the message based on the number of selected items
                val selected2Count = adapter.getSelectedItemCount()
                val message = if (selected2Count > 1) {
                    "Are you sure you want to remove these Musics?"
                } else {
                    "Are you sure you want to remove this Music?"
                }
                messageTextView.text = message

                // Set the cancel button to dismiss the dialog
                cancelButton.setOnClickListener {
                    alertDialog.dismiss()
                }

                // Set the remove button to perform the removal action
                removeButton.setOnClickListener {

                    // Launch a coroutine to remove selected items from the database
                    lifecycleScope.launch {
                        val selectedSongs = adapter.getSelectedSongs()
                        // Delete each selected song from the database
                        for (song in selectedSongs) {
                            musicDatabase.musicFavDao()
                                .deleteMusic(song.toMusicFavEntity())
                        }
                        // Remove the selected songs from the list and update the adapter
                        favouriteSongs.removeAll(selectedSongs)
                        adapter.notifyDataSetChanged()

                        // Check if the favouriteSongs list is empty after removal
                        if (favouriteSongs.isEmpty()) {
                            // Disable the multiple item select listener and exit selection mode
                            binding.playlistToolbar.menu.findItem(R.id.checkMulti).isEnabled = false
                            exitSelectionMode()
                        } else {
                            adapter.deselectAll()
                            binding.playlistToolbar.menu.findItem(R.id.checkMulti).isEnabled = true
                        }
                        shuffleEmpty()
                        alertDialog.dismiss()
                    }

                }

                // Show the AlertDialog
                alertDialog.show()

            }
            // Set up menu item click listener
            binding.playlistToolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.checkMulti -> {
                        if (isAllSelected) {
                            adapter.deselectAll()
                            isAllSelected = false
                            menuItem.icon = ContextCompat.getDrawable(this, R.drawable.round_crop_square_24)
                        } else {
                            adapter.selectAll()
                            isAllSelected = true
                            menuItem.icon = ContextCompat.getDrawable(this, R.drawable.check_box_24)

                        }
                        true
                    }


                    else -> false
                }
            }
        } else {
            // Hide the bottom toolbar
            binding.bottomToolbarF.visibility = View.GONE
            binding.playlistToolbar.title = "FAVOURITES"
            binding.playlistToolbar.menu.clear()
            binding.playlistToolbar.inflateMenu(R.menu.more_playlist)
            binding.playlistToolbar.setNavigationOnClickListener {
                finish()
            }
            // Set up menu item click listener only if there are favorite songs
            binding.playlistToolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.MorePlaylist -> {
                        showBottomSheetDialog()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun checkAndRemoveDeletedSongs() {
        lifecycleScope.launch {
            val iterator = favouriteSongs.iterator()
            var songsRemoved = false

            while (iterator.hasNext()) {
                val song = iterator.next()
                val file = File(song.path)
                if (!file.exists()) {
                    // Song file doesn't exist, remove from favorites list and database
                    iterator.remove()
                    musicDatabase.musicFavDao().deleteMusic(song.toMusicFavEntity())
                    songsRemoved = true
                }
            }

            if (songsRemoved) {
                adapter.notifyDataSetChanged()
                shuffleEmpty()  // Update UI if songs were removed
            }
        }
    }

    // Show dialog to add selected songs to a playlist
    private fun showAddToPlaylistDialog(selectedSongs: List<Music>) {
        val bottomSheetPLDialog = BottomSheetDialog(this)
        val bottomSheetPLView = LayoutInflater.from(this).inflate(
            R.layout.add_to_playlist_bottom_sheet, null
        )
        bottomSheetPLDialog.setContentView(bottomSheetPLView)

        val createPlaylistButton = bottomSheetPLView.findViewById<Button>(R.id.create_playlist_button)
        val playlistRecyclerView = bottomSheetPLView.findViewById<RecyclerView>(R.id.playlistRecyclerview)

        playlistRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = DatabaseClientMusic.getInstance(this@FavouriteActivity).playlistMusicDao()
            val playlists = dao.getAllPlaylists().map { mapEntityToPlaylistMusic(it) }

            withContext(Dispatchers.Main) {
                val playlistAdapter = AddToPlaylistViewAdapter(
                    context = this@FavouriteActivity,
                    playlists = playlists.toMutableList(),
                    selectedMusic = null, // Handle multiple songs
                    bottomSheetPLDialog = bottomSheetPLDialog,
                    selectedSongs = selectedSongs // Pass the selected songs
                )
                playlistRecyclerView.adapter = playlistAdapter
            }
        }

        createPlaylistButton.setOnClickListener {
            // Inflate the new bottom sheet layout for creating a playlist
            val createPlaylistView = LayoutInflater.from(this).inflate(
                R.layout.video_playlist_bottom_dialog, null
            )

            val createPlaylistDialog = BottomSheetDialog(this)
            createPlaylistDialog.setContentView(createPlaylistView)

            // Find the views in the create playlist bottom sheet layout
            val renameField = createPlaylistView.findViewById<TextInputEditText>(R.id.renameField)
            val createButton = createPlaylistView.findViewById<Button>(R.id.button_create_playlist)

            renameField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrEmpty()) {
                        createButton.setBackgroundColor(ContextCompat.getColor(this@FavouriteActivity, R.color.button_background_default))
                    } else {
                        createButton.setBackgroundColor(ContextCompat.getColor(this@FavouriteActivity, R.color.cool_blue))
                    }
                }
            })

            createButton.setOnClickListener {
                val playlistName = renameField.text.toString().trim()
                if (playlistName.isNotEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val dao = DatabaseClientMusic.getInstance(this@FavouriteActivity).playlistMusicDao()

                        // Create a new playlist entity
                        val newPlaylist = PlaylistMusicEntity(
                            name = playlistName
                        )

                        // Insert the new playlist into the database and get its ID
                        val playlistId = dao.insertPlaylist(newPlaylist)

                        // Add the selected songs to the newly created playlist
                        val crossRefs = selectedSongs.map { song ->
                            PlaylistMusicCrossRef(
                                playlistMusicId = playlistId,
                                musicId = song.id // Use song.id to reference the music
                            )
                        }
                        dao.insertPlaylistMusicCrossRef(crossRefs)


                        withContext(Dispatchers.Main) {
                            // Dismiss the dialogs
                            createPlaylistDialog.dismiss()
                            bottomSheetPLDialog.dismiss()
                            val songCount = selectedSongs.size
                            Toast.makeText(this@FavouriteActivity, "$songCount song(s) added to the playlist '$playlistName'", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Handle empty name case (e.g., show an error message)
                    Toast.makeText(this@FavouriteActivity, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

            createPlaylistDialog.show()
            bottomSheetPLDialog.dismiss()
        }

        bottomSheetPLDialog.show()
    }
    private fun mapEntityToPlaylistMusic(entity: PlaylistMusicEntity): PlaylistMusic {
        return PlaylistMusic(
            id = entity.id,
            name = entity.name,
            music = listOf() // Initialize with an empty list or fetch actual music if needed
        )
    }
    private fun exitSelectionMode() {
        adapter.clearSelectionMode()
        onSelectionModeChanged(isSelectionMode = false, selectedCount = 0, totalCount = favouriteSongs.size)
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(favouritesUpdatedReceiver)
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (adapter.isSelectionMode) {
            // Exit selection mode without finishing the activity
            exitSelectionMode()
        } else {
            super.onBackPressed()

        }
    }

}

// Extension function to convert Music to MusicFavEntity
fun Music.toMusicFavEntity(): MusicFavEntity {
    return MusicFavEntity(
        id = this.id,
        title = this.title,
        album = this.album,
        artist = this.artist,
        duration = this.duration,
        path = this.path,
        size = this.size.toString(),
        artUri = this.artUri,
        dateAdded = this.dateAdded
    )
}


// Extension function to convert MusicFavEntity to Music
fun MusicFavEntity.toMusic(): Music {
    return Music(
        id = this.id,
        title = this.title,
        album = this.album,
        artist = this.artist,
        duration = this.duration,
        path = this.path,
        size = this.size,
        artUri = this.artUri,
        dateAdded = this.dateAdded!!
    )
}