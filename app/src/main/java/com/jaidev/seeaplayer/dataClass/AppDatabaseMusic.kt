package com.jaidev.seeaplayer.dataClass

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [PlaylistMusicEntity::class, MusicEntity::class, PlaylistMusicCrossRef::class], version = 7)

abstract class AppDatabaseMusic : RoomDatabase() {
    abstract fun playlistMusicDao(): PlaylistMusicDao
}

