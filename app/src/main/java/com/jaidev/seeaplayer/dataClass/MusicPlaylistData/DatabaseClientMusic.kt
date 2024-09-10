package com.jaidev.seeaplayer.dataClass.MusicPlaylistData

import android.content.Context
import androidx.room.Room

object DatabaseClientMusic {
    @Volatile
    private var INSTANCE: AppDatabaseMusic? = null

    fun getInstance(context: Context): AppDatabaseMusic {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabaseMusic::class.java,
                "playlist-music-database"
            )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}