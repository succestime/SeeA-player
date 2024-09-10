package com.jaidev.seeaplayer.dataClass.MusicFavouriteData

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MusicFavDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusic(music: MusicFavEntity)

    @Query("SELECT * FROM music_table")
    suspend fun getAllMusic(): List<MusicFavEntity>

    @Delete
    suspend fun deleteMusic(music: MusicFavEntity)


    @Query("DELETE FROM music_table")
    suspend fun deleteAllMusic()  // New method to delete all songs
}
