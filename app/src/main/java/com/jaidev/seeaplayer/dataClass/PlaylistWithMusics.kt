package com.jaidev.seeaplayer.dataClass



import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithMusics(
    @Embedded val playlistMusic: PlaylistMusicEntity,
    @Relation(
        parentColumn = "musicid",
        entityColumn = "musicid",
        associateBy = Junction(
            value = PlaylistMusicCrossRef::class,
            parentColumn = "playlistMusicId",
            entityColumn = "musicId"
        )
    )
    val music: List<MusicEntity>
)
