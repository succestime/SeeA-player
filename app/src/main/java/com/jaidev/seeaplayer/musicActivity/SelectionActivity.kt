
package com.jaidev.seeaplayer.musicActivity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.MusicAdapter
import com.jaidev.seeaplayer.databinding.ActivitySelectionBinding

class SelectionActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectionBinding
    private lateinit var adapter: MusicAdapter
    private lateinit var selectionConstraintlayout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
//        setTheme(More.themesList[More.themeIndex])
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.selectionRV.setHasFixedSize(true)
        binding.selectionRV.setItemViewCacheSize(30)
        binding.selectionRV.layoutManager = LinearLayoutManager(this,)
        adapter = MusicAdapter(this, MainActivity.MusicListMA,  selectionActivity = true )
        binding.selectionRV.adapter = adapter
        binding.backBtnSA.setOnClickListener { finish() }

        // for search View
        binding.searchViewSA.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean =true
            override fun onQueryTextChange(newText: String?): Boolean {
                MainActivity.musicListSearch = ArrayList()
                if (newText != null){
                    val userInput = newText.lowercase()
                    for (song in MainActivity.MusicListMA)
                        if (song.title.lowercase().contains(userInput))
                            MainActivity.musicListSearch.add(song)
                    MainActivity.search = true
                    adapter.updateMusicList(searchList = MainActivity.musicListSearch)
                }
                return true
            }
        })

        selectionConstraintlayout = binding.selectionConstraintlayout
        setSwipeRefreshBackgroundColor()
    }
    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            selectionConstraintlayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)


        } else {
            // Light mode is enabled, set background color to white
            selectionConstraintlayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR


        }
    }

}
