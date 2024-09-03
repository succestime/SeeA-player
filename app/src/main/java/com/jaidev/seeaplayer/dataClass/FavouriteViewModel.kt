package com.jaidev.seeaplayer.dataClass

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jaidev.seeaplayer.musicActivity.toMusic
import com.jaidev.seeaplayer.musicActivity.toMusicFavEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FavouriteViewModel(application: Application) : AndroidViewModel(application) {

    private val musicDatabase = MusicFavDatabase.getDatabase(application)
    private val _favouriteSongs = MutableLiveData<List<Music>>()
    val favouriteSongs: LiveData<List<Music>> get() = _favouriteSongs

    init {
        loadFavouriteSongs()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFavouriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val favoriteEntities = musicDatabase.musicFavDao().getAllMusic()
            _favouriteSongs.postValue(favoriteEntities.map { it.toMusic() })
        }
    }

    fun refreshFavourites() {
        loadFavouriteSongs()
    }

    fun removeDeletedSongs() {
        viewModelScope.launch {
            val updatedSongs = _favouriteSongs.value?.filter { File(it.path).exists() } ?: listOf()
            _favouriteSongs.postValue(updatedSongs)

            // Remove from database as well
            val deletedSongs = _favouriteSongs.value?.filterNot { File(it.path).exists() } ?: listOf()
            deletedSongs.forEach { musicDatabase.musicFavDao().deleteMusic(it.toMusicFavEntity()) }
        }
    }
}
