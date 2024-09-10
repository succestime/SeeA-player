package com.jaidev.seeaplayer.dataClass.MP3Data


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MP3FileDao {
    @Insert
    suspend fun insert(mp3File: MP3FileEntity)

    @Query("SELECT * FROM mp3_files")
    suspend fun getAllMP3Files(): List<MP3FileEntity>

    @Query("DELETE FROM mp3_files WHERE id = :id")
    suspend fun deleteById(id: String)
}
