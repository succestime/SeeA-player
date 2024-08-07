package com.jaidev.seeaplayer.dataClass

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [PlaylistEntity::class, VideoEntity::class, PlaylistVideoCrossRef::class], version = 3) // Update the version if needed
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}