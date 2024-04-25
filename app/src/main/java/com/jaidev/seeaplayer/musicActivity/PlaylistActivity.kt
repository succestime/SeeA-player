
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.PlaylistViewAdapter
import com.jaidev.seeaplayer.dataClass.MusicPlaylist
import com.jaidev.seeaplayer.dataClass.Playlist
import com.jaidev.seeaplayer.databinding.ActivityPlaylistBinding
import com.jaidev.seeaplayer.databinding.AddPlaylistDialogBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaylistActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter : PlaylistViewAdapter
    private lateinit var playListLayout: ConstraintLayout
    private var mInterstitialAd : InterstitialAd? = null
    companion object{
        var musicPlaylist : MusicPlaylist = MusicPlaylist()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        supportActionBar?.title = "Playlist"
        setContentView(binding.root)
        loadAd()

        binding.playlistRV.setHasFixedSize(true)
        binding.playlistRV.setItemViewCacheSize(10)
        binding.playlistRV.layoutManager = GridLayoutManager(this@PlaylistActivity,2)
        adapter = PlaylistViewAdapter(this , playlistList = musicPlaylist.ref)
        binding.playlistRV.adapter = adapter
        binding.addPlaylistBtn.setOnClickListener { customAlertDialog() }



        setActionBarGradient()
        playListLayout = binding.playlistLayout

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
            playListLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            playListLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }

    private fun setActionBarGradient() {
        // Check the current night mode
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Light mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@PlaylistActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else {
            // Dark mode is applied or the mode is set to follow system
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@PlaylistActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        }
    }

    private fun customAlertDialog(){
        val customDialog = LayoutInflater.from(this@PlaylistActivity).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(customDialog)
            .setTitle("Playlist Details")
            .setPositiveButton("ADD"){ dialog, _ ->
                val playlistName = binder.playlistName.text
                val createdBy = binder.yourName.text
                if(playlistName != null && createdBy != null)
                    if(playlistName.isNotEmpty() && createdBy.isNotEmpty())
                    {
                        addPlaylist(playlistName.toString(), createdBy.toString())
                    }
                loadAd()
                mInterstitialAd?.show(this)
                dialog.dismiss()
            }.show()

    }
    private fun addPlaylist(name: String, createdBy: String) {
        val playlistExists = musicPlaylist.ref.any { it.name == name }
        if (playlistExists) {
            Toast.makeText(this, "Playlist Exists!", Toast.LENGTH_SHORT).show()
        } else {
            val tempPlaylist = Playlist().apply {
                this.name = name
                this.playlist = ArrayList()
                this.createdBy = createdBy
                this.createdOn = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Calendar.getInstance().time)
            }
            musicPlaylist.ref.add(tempPlaylist)

            // Save updated playlists to SharedPreferences

            adapter.refreshPlaylist()
        }
    }


    fun loadAd(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })

    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }


}