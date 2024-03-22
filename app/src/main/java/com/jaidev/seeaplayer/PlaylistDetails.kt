package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.checkPlaylist
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
        setTheme(R.style.coolBlueNav)
        supportActionBar?.title = "Playlist Album"
        binding = ActivityPlatylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentPlaylistPos = intent.extras?.get("index") as Int
        PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
            checkPlaylist(playlist =  PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        binding.playlistDetailsRV.setHasFixedSize(true)
        binding.playlistDetailsRV.setItemViewCacheSize(13)
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this,)
        adapter = MusicAdapter(this, PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist,  playlistDetails = true)
        binding.playlistDetailsRV.adapter = adapter

        binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerMusicActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }

        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this , SelectionActivity::class.java))
        }

binding.removeAllPD.setOnClickListener {
    val builder = MaterialAlertDialogBuilder(this)
    builder.setTitle("Remove")
        .setMessage("Do you want to remove all songs from playlist?")
        .setPositiveButton("Yes"){ dialog, _ ->
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
            adapter.refreshPlaylist()
            dialog.dismiss()
        }
        .setNegativeButton("No"){dialog, _ ->
            dialog.dismiss()
        }
    val customDialog = builder.create()
    customDialog.show()
    customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN)

         }

        supportActionBar?.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@PlaylistDetails,
                    R.drawable.background_actionbar
                )
            )
        }
        setActionBarGradient()
        playlistdetailsLayout = binding.playlistDetailsLayout

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
}

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            playlistdetailsLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            playlistdetailsLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@PlaylistDetails,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        binding.moreInfoPD.text = "Total ${adapter.itemCount} Songs.\n\n" +
                "Created On: ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn}\n\n" +
                "  -- ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdBy}"

        if(adapter.itemCount > 0)
        {
            Glide.with(this)
                .load(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.speaker).centerCrop())
                .into(binding.playlistImgPD)
            binding.shuffleBtnPD.visibility = View.VISIBLE
        }
       adapter.notifyDataSetChanged()

    }
}