package com.jaidev.seeaplayer.dataClass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_table")
data class MusicFavEntity(
    @PrimaryKey val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val size: String,
    val artUri: String,
    val dateAdded: Long?
)
