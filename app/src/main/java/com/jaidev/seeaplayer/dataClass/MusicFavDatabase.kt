package com.jaidev.seeaplayer.dataClass

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MusicFavEntity::class], version = 5, exportSchema = false)
abstract class MusicFavDatabase : RoomDatabase() {

    abstract fun musicFavDao(): MusicFavDao

    companion object {
        @Volatile
        private var INSTANCE: MusicFavDatabase? = null

        fun getDatabase(context: Context): MusicFavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicFavDatabase::class.java,
                    "music_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
