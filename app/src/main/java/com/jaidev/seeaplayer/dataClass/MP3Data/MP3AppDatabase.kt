package com.jaidev.seeaplayer.dataClass.MP3Data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MP3FileEntity::class], version = 1)
abstract class MP3AppDatabase : RoomDatabase() {
    abstract fun mp3FileDao(): MP3FileDao

    companion object {
        @Volatile
        private var INSTANCE: MP3AppDatabase? = null

        fun getDatabase(context: Context): MP3AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MP3AppDatabase::class.java,
                    "mp3_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
