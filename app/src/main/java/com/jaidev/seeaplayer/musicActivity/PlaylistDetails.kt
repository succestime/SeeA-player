
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.dataClass.checkPlaylist
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.databinding.ActivityPlatylistDetailsBinding

class PlaylistDetails : AppCompatActivity() {
    lateinit var binding: ActivityPlatylistDetailsBinding
    lateinit var adapter: MusicAdapter
    private lateinit var playlistdetailsLayout: ConstraintLayout

    companion object {
        var currentPlaylistPos: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        supportActionBar?.title = "Playlist Album"
        binding = ActivityPlatylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button
        currentPlaylistPos = intent.extras?.get("index") as Int
        try {
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        } catch (_: Exception) {}

        binding.playlistDetailsRV.setHasFixedSize(true)
        binding.playlistDetailsRV.setItemViewCacheSize(50)
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this)
        adapter = MusicAdapter(this, PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist, playlistDetails = true)
        binding.playlistDetailsRV.adapter = adapter

        shuffleAddRemove()
//        setActionBarGradient()
        playlistdetailsLayout = binding.playlistDetailsLayout

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun shuffleAddRemove(){
        binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerMusicActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }

        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this , SelectionActivity::class.java))
        }
// Disable click events for addBtnPD


        // Disable click events for removeAllPD
        binding.removeAllPD.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Remove")
                .setMessage("Do you want to remove all songs from playlist?")
                .setPositiveButton("Yes") { dialog,_ ->
                    PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
                    adapter.refreshPlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()
        }


    }

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            playlistdetailsLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            playlistdetailsLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
    }

//    private fun setActionBarGradient() {
//        // Check the current night mode
//        val nightMode = AppCompatDelegate.getDefaultNightMode()
//        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
//            // Light mode is applied
//            supportActionBar?.apply {
//                setBackgroundDrawable(
//                    ContextCompat.getDrawable(
//                        this@PlaylistDetails,
//                        R.drawable.background_actionbar_light
//                    )
//                )
//            }
//        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
//            // Dark mode is applied
//            supportActionBar?.apply {
//                setBackgroundDrawable(
//                    ContextCompat.getDrawable(
//                        this@PlaylistDetails,
//                        R.drawable.background_actionbar
//                    )
//                )
//            }
//        } else {
//            // System Default mode is applied
//            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
//                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
//                else -> false
//            }
//            // Set the ActionBar color based on the System Default mode
//            if (isSystemDefaultDarkMode) {
//                // System Default mode is dark
//                supportActionBar?.apply {
//                    setBackgroundDrawable(
//                        ContextCompat.getDrawable(
//                            this@PlaylistDetails,
//                            R.drawable.background_actionbar
//                        )
//                    )
//                }
//            } else {
//                // System Default mode is light
//                supportActionBar?.apply {
//                    setBackgroundDrawable(
//                        ContextCompat.getDrawable(
//                            this@PlaylistDetails,
//                            R.drawable.background_actionbar_light
//                        )
//                    )
//                }
//            }
//        }
//    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        binding.moreInfoPD.text = "Total ${adapter.itemCount} Songs.\n\n" +
                "Created On: ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn}"

        if (adapter.itemCount > 0) {
            Glide.with(this)

                .load(getImgArt(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].path))
                .apply(RequestOptions()
                    .error(R.drawable.music_speaker_three) // Use the newly created drawable
                    .centerCrop())
                .into(binding.playlistImgPD)
            binding.shuffleBtnPD.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }
}