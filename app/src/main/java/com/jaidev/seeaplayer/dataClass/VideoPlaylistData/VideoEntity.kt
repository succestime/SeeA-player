package com.jaidev.seeaplayer.dataClass.VideoPlaylistData

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Long = 0,
    val folderName: String,
    val size: String,
    val path: String,
    val artUri: String,
    val dateAdded: Long,
    val isNew: Boolean = false,
    val isPlayed: Boolean = false
)
