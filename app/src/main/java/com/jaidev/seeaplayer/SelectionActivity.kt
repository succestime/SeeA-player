package com.jaidev.seeaplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaidev.seeaplayer.MusicAdapter
import com.jaidev.seeaplayer.databinding.ActivitySelectionBinding

class SelectionActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectionBinding
    private lateinit var adapter: MusicAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueNav)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.selectionRV.setHasFixedSize(true)
        binding.selectionRV.setItemViewCacheSize(13)
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
    }

}