package com.jaidev.seeaplayer.dataClass

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["playlistMusicId", "musicId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistMusicEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistMusicId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MusicEntity::class,
            parentColumns = ["id"],
            childColumns = ["musicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistMusicCrossRef(
    val playlistMusicId: Long,
    val musicId: String
)


