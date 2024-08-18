
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
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
        setSwipeRefreshBackgroundColor()


        loadFavouriteSongs()
    }

    override fun onResume() {
        super.onResume()
        if (favouritesChanged) {
            adapter.updateFavourites(favouriteSongs)
            favouritesChanged = false
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
        }
    }



    private fun shuffleEmpty() {
        if (favouriteSongs.size < 1) binding.shuffleBtnFA.visibility = View.INVISIBLE

        binding.shuffleBtnFA.setOnClickListener {
            val intent = Intent(this, PlayerMusicActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouriteShuffle")
            startActivity(intent)
        }

        // Update visibility of emptyStateLayout
        if (favouriteSongs.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
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
                        // Update the UI based on whether there are any favorites left
                        shuffleEmpty()
                        // Check if the favouriteSongs list is empty after removal
                        if (favouriteSongs.isEmpty()) {
                            // Disable the multiple item select listener and exit selection mode
                            binding.playlistToolbar.menu.findItem(R.id.checkMulti).isEnabled = false
                            exitSelectionMode()
                        } else {
                            adapter.deselectAll()
                            binding.playlistToolbar.menu.findItem(R.id.checkMulti).isEnabled = true
                        }
                        // Dismiss the dialog after the action
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
            id = entity.musicid,
            name = entity.name,
            music = listOf() // Initialize with an empty list or fetch actual music if needed
        )
    }
    private fun exitSelectionMode() {
        adapter.clearSelectionMode()
        onSelectionModeChanged(isSelectionMode = false, selectedCount = 0, totalCount = favouriteSongs.size)
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
