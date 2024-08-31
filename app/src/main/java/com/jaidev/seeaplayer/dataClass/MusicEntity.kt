package com.jaidev.seeaplayer.dataClass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music")
data class MusicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val album: String,
    val artist: String, val duration: Long = 0, var path: String,
    val size: String,
    var artUri: String,
    val dateAdded: Long?
)
