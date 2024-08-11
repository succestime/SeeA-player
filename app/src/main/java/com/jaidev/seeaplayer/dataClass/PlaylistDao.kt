package com.jaidev.seeaplayer.dataClass

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideo(videoEntity: VideoEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM videos WHERE id = :videoId)")
    suspend fun videoExists(videoId: String): Boolean

    @Insert
    suspend fun insertPlaylistVideoCrossRef(crossRef: PlaylistVideoCrossRef)

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithVideos(playlistId: Long): PlaylistWithVideos

    @Query("SELECT COUNT(*) FROM videos WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)")
    suspend fun getVideoCountForPlaylist(playlistId: Long): Int


    @Query("SELECT SUM(duration) FROM videos WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)")
    suspend fun getTotalDurationForPlaylist(playlistId: Long): Long

    @Query("""
        SELECT artUri FROM videos 
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId) 
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUri(playlistId: Long): String?


    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long) // Add this method to delete a playlist by ID


    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
    """)
    suspend fun getVideosForPlaylist(playlistId: Long): List<String>

    @Query("DELETE FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deleteVideoFromPlaylist(playlistId: Long, videoId: String)


    @Query("DELETE FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId")
    suspend fun deleteAllVideosFromPlaylist(playlistId: Long)

    @Query("SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId")
    suspend fun getVideoIdsForPlaylist(playlistId: Long): List<String>

    @Query("SELECT COUNT(*) > 0 FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun isVideoInPlaylist(playlistId: Long, videoId: String): Boolean




}

