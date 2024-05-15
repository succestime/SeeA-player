
package com.jaidev.seeaplayer.musicActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jaidev.seeaplayer.PlayerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.FavouriteAdapter
import com.jaidev.seeaplayer.dataClass.Music
import com.jaidev.seeaplayer.dataClass.checkPlaylist
import com.jaidev.seeaplayer.databinding.ActivityFavouriteBinding

class FavouriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouriteBinding
    private lateinit var adapter: FavouriteAdapter
    private lateinit var favouritelayout: ConstraintLayout


    companion object{
        var favouritesChanged: Boolean = false
        var favouriteSongs: ArrayList<Music> = ArrayList()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        supportActionBar?.title = "Favourite"
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the Up button
        favouriteSongs = checkPlaylist(favouriteSongs)
        binding.favouriteRV.setHasFixedSize(true)
        binding.favouriteRV.setItemViewCacheSize(13)
        binding.favouriteRV.layoutManager = GridLayoutManager(this, 3)
        adapter = FavouriteAdapter(this, favouriteSongs)
        binding.favouriteRV.adapter = adapter

        favouritesChanged = false

        shuffleEmpty()
        setActionBarGradient()
        favouritelayout = binding.favouriteLayout
        // Set the background color of SwipeRefreshLayout based on app theme
        setSwipeRefreshBackgroundColor()
        loadFavouriteSongs()
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun shuffleEmpty(){
        if(favouriteSongs.size < 1) binding.shuffleBtnFA.visibility = View.INVISIBLE

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
            // Dark mode is enabled, set background color to #012030
            favouritelayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            favouritelayout.setBackgroundColor(resources.getColor(android.R.color.white))
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
                        this@FavouriteActivity,
                        R.drawable.background_actionbar_light
                    )
                )
            }
        } else if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark mode is applied
            supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@FavouriteActivity,
                        R.drawable.background_actionbar
                    )
                )
            }
        } else {
            // System Default mode is applied
            val isSystemDefaultDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
                else -> false
            }
            // Set the ActionBar color based on the System Default mode
            if (isSystemDefaultDarkMode) {
                // System Default mode is dark
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FavouriteActivity,
                            R.drawable.background_actionbar
                        )
                    )
                }
            } else {
                // System Default mode is light
                supportActionBar?.apply {
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            this@FavouriteActivity,
                            R.drawable.background_actionbar_light
                        )
                    )
                }
            }
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        // ... other code
        if (favouritesChanged) {
            // Update the adapter with the new list of favourite songs
            adapter.updateFavourites(favouriteSongs)
            // Reset the flag
            favouritesChanged = true
            // Save favourite songs to SharedPreferences
            saveFavouriteSongs()
            loadFavouriteSongs()
        }

            binding.shuffleBtnFA.setOnClickListener {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("index", 0)
                intent.putExtra("class", "FavouriteShuffle")
                startActivity(intent)
            }

    }
    private fun saveFavouriteSongs() {
        val editor = getSharedPreferences("FAVOURITES", AppCompatActivity.MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(favouriteSongs)
        Log.d("SaveFavourite", "Saving favourite songs: $jsonString")
        editor.putString("FavouriteAdapter", jsonString)
        editor.apply()
    }

    private fun loadFavouriteSongs() {
        val sharedPreferences = getSharedPreferences("FAVOURITES", AppCompatActivity.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("FavouriteAdapter", null)
        jsonString?.let {
            Log.d("LoadFavourite", "Loaded favourite songs: $jsonString")
            try {
                val type = object : TypeToken<ArrayList<Music>>() {}.type
                favouriteSongs = GsonBuilder().create().fromJson(it, type)
                // Update the adapter with loaded favorite songs
                adapter.updateFavourites(favouriteSongs)
            } catch (e: Exception) {
                Log.e("ParsingException", "Error parsing JSON: ${e.message}", e)
            }
        }
    }


}
