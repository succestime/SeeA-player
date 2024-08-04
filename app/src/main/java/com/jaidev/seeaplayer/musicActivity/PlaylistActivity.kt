
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.PlaylistViewAdapter
import com.jaidev.seeaplayer.dataClass.MusicPlaylist
import com.jaidev.seeaplayer.dataClass.Playlist
import com.jaidev.seeaplayer.dataClass.ThemeHelper
import com.jaidev.seeaplayer.databinding.ActivityPlaylistBinding
import com.jaidev.seeaplayer.databinding.AddPlaylistDialogBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaylistActivity : AppCompatActivity() , PlaylistViewAdapter.OnItemClickListener{
    lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter : PlaylistViewAdapter
    private lateinit var playListLayout: ConstraintLayout
    private var mInterstitialAd : InterstitialAd? = null
    companion object{
        var musicPlaylist : MusicPlaylist = MusicPlaylist()
    }

    // SharedPreferences keys
    private val PLAYLIST_PREFS = "playlist_prefs"
    private val PLAYLIST_KEY = "playlists"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeHelper.getSavedTheme(this)
        ThemeHelper.applyTheme(this,theme)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        supportActionBar?.title = "Playlist"
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button
        loadAd()

        binding.playlistRV.setHasFixedSize(true)
        binding.playlistRV.setItemViewCacheSize(50)
        binding.playlistRV.layoutManager = LinearLayoutManager(this@PlaylistActivity)

        adapter = PlaylistViewAdapter(this , playlistList = musicPlaylist.ref , this)
        binding.playlistRV.adapter = adapter
binding.addPlaylistBtn.setOnClickListener {
    customAlertDialog()
}
//
//        setActionBarGradient()
        playListLayout = binding.playlistLayout

        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Load playlists from SharedPreferences

    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            playListLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            playListLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
    }
    override fun onItemClick(position: Int) {
        val intent = Intent(this, PlaylistDetails::class.java)
        intent.putExtra("index", position)
        startActivity(intent)
        // Add transition animation here
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }
//    private fun setActionBarGradient() {
//        // Check the current night mode
//        val nightMode = AppCompatDelegate.getDefaultNightMode()
//        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
//            // Light mode is applied
//            supportActionBar?.apply {
//                setBackgroundDrawable(
//                    ContextCompat.getDrawable(
//                        this@PlaylistActivity,
//                        R.drawable.background_actionbar_light
//                    )
//                )
//            }
//        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
//            // Dark mode is applied
//            supportActionBar?.apply {
//                setBackgroundDrawable(
//                    ContextCompat.getDrawable(
//                        this@PlaylistActivity,
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
//                            this@PlaylistActivity,
//                            R.drawable.background_actionbar
//                        )
//                    )
//                }
//            } else {
//                // System Default mode is light
//                supportActionBar?.apply {
//                    setBackgroundDrawable(
//                        ContextCompat.getDrawable(
//                            this@PlaylistActivity,
//                            R.drawable.background_actionbar_light
//                        )
//                    )
//                }
//            }
//        }
//    }


    private fun customAlertDialog(){
        val customDialog = LayoutInflater.from(this@PlaylistActivity).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(customDialog)
            .setTitle("Playlist Details")
            .setPositiveButton("ADD"){ dialog, _ ->
                val playlistName = binder.playlistName.text
                val createdBy = binder.playlistName.text
                if(playlistName != null && createdBy != null)
                    if(playlistName.isNotEmpty() && createdBy.isNotEmpty()) {
                        addPlaylist(playlistName.toString(), createdBy.toString())
                    }
                loadAd()
                mInterstitialAd?.show(this)
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()

        // Set the positive button color to cool_blue
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.cool_blue))
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


            adapter.refreshPlaylist()
        }
        // Update visibility of emptyStateLayout
        if (musicPlaylist.ref.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    fun loadAd(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3504589383575544/2737550146", adRequest, object : InterstitialAdLoadCallback() {
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

        // Update visibility of emptyStateLayout
        if (musicPlaylist.ref.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
    }



}
