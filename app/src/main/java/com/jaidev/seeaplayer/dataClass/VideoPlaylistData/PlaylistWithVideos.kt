package com.jaidev.seeaplayer.dataClass.VideoPlaylistData



import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithVideos(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistVideoCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "videoId"
        )
    )
    val videos: List<VideoEntity>
)
