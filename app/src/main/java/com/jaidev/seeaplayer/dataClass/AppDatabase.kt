package com.jaidev.seeaplayer.dataClass

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [PlaylistEntity::class, VideoEntity::class, PlaylistVideoCrossRef::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
