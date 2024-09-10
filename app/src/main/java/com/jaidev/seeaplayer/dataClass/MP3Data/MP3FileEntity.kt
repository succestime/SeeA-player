package com.jaidev.seeaplayer.dataClass.MP3Data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mp3_files")
data class MP3FileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Long,
    val size: String,
    val dateAdded: Long?,
    val path: String,
    var artUri: String
)
