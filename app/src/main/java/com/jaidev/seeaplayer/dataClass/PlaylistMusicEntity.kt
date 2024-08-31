package com.jaidev.seeaplayer.dataClass

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jaidev.seeaplayer.PlaylistVideoActivity

@Entity(tableName = "musicPlaylists")
data class PlaylistMusicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: PlaylistVideoActivity.SortType = PlaylistVideoActivity.SortType.TITLE_ASC // default value


)
