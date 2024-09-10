package com.jaidev.seeaplayer.dataClass.MusicFavouriteData

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_table")
data class MusicFavEntity(
    @PrimaryKey val musicid: String,
    val title: String,
    val album: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val size: String,
    val artUri: String,
    val dateAdded: Long?
)
