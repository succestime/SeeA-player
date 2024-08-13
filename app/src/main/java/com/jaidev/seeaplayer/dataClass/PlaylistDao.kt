package com.jaidev.seeaplayer.dataClass

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jaidev.seeaplayer.PlaylistVideoActivity

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

    @Transaction
    suspend fun getFirstVideoImageUri(playlistId: Long, sortOrder: PlaylistVideoActivity.SortType): String? {
        return when (sortOrder) {
            PlaylistVideoActivity.SortType.TITLE_ASC -> getFirstVideoImageUriByTitleAsc(playlistId)
            PlaylistVideoActivity.SortType.TITLE_DESC -> getFirstVideoImageUriByTitleDesc(playlistId)
            PlaylistVideoActivity.SortType.DURATION_ASC -> getFirstVideoImageUriByDurationAsc(playlistId)
            PlaylistVideoActivity.SortType.DURATION_DESC -> getFirstVideoImageUriByDurationDesc(playlistId)
            PlaylistVideoActivity.SortType.DATE_OLDEST -> getFirstVideoImageUriByDateAddedAsc(playlistId)
            PlaylistVideoActivity.SortType.DATE_NEWEST -> getFirstVideoImageUriByDateAddedDesc(playlistId)
            PlaylistVideoActivity.SortType.SIZE_SMALLEST -> getFirstVideoImageUriBySizeAsc(playlistId)
            PlaylistVideoActivity.SortType.SIZE_LARGEST -> getFirstVideoImageUriBySizeDesc(playlistId)
        }
    }
    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.title COLLATE NOCASE ASC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByTitleAsc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.title COLLATE NOCASE DESC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByTitleDesc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.duration ASC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByDurationAsc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.duration DESC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByDurationDesc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.dateAdded ASC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByDateAddedAsc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.dateAdded DESC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriByDateAddedDesc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.size ASC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriBySizeAsc(playlistId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
        ORDER BY v.size DESC
        LIMIT 1
    """)
    suspend fun getFirstVideoImageUriBySizeDesc(playlistId: Long): String?

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long) // Add this method to delete a playlist by ID


    @Query(
        """
        SELECT v.artUri 
        FROM videos v
        JOIN PlaylistVideoCrossRef pv ON v.id = pv.videoId
        WHERE pv.playlistId = :playlistId
    """
    )
    suspend fun getVideosForPlaylist(playlistId: Long): List<String>

    @Query("DELETE FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deleteVideoFromPlaylist(playlistId: Long, videoId: String)


    @Query("DELETE FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId")
    suspend fun deleteAllVideosFromPlaylist(playlistId: Long)

    @Query("SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId")
    suspend fun getVideoIdsForPlaylist(playlistId: Long): List<String>

    @Query("SELECT COUNT(*) > 0 FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun isVideoInPlaylist(playlistId: Long, videoId: String): Boolean

    // Sort by title A-Z
    // Sort by title A-Z
    @Query(
        """
    SELECT * FROM videos
    WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
    ORDER BY title COLLATE NOCASE ASC
"""
    )
    suspend fun getVideosSortedByTitleAsc(playlistId: Long): List<VideoEntity>

    // Sort by title Z-A
    @Query(
        """
    SELECT * FROM videos
    WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
    ORDER BY title COLLATE NOCASE DESC
"""
    )
    suspend fun getVideosSortedByTitleDesc(playlistId: Long): List<VideoEntity>

    // Sort by duration longest first
    @Query(
        """
        SELECT * FROM videos
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
        ORDER BY duration DESC
    """
    )
    suspend fun getVideosSortedByDurationDesc(playlistId: Long): List<VideoEntity>


    // Sort by duration ascending (shortest duration first)
    @Query(
        """
    SELECT * FROM videos
    WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
    ORDER BY duration ASC
"""
    )
    suspend fun getVideosSortedByDurationAsc(playlistId: Long): List<VideoEntity>


    // Sort by newest video first
    @Query(
        """
        SELECT * FROM videos
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
        ORDER BY dateAdded DESC
    """
    )
    suspend fun getVideosSortedByNewest(playlistId: Long): List<VideoEntity>


    // Sort by oldest video first
    @Query(
        """
        SELECT * FROM videos
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
        ORDER BY dateAdded ASC
    """
    )
    suspend fun getVideosSortedByOldest(playlistId: Long): List<VideoEntity>

    // Sort by size largest first
    @Query(
        """
        SELECT * FROM videos
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
        ORDER BY size DESC
    """
    )
    suspend fun getVideosSortedBySizeDesc(playlistId: Long): List<VideoEntity>

    // Sort by size smallest first
    @Query(
        """
        SELECT * FROM videos
        WHERE id IN (SELECT videoId FROM PlaylistVideoCrossRef WHERE playlistId = :playlistId)
        ORDER BY size ASC
    """
    )
    suspend fun getVideosSortedBySizeAsc(playlistId: Long): List<VideoEntity>

    @Query("UPDATE playlists SET sortOrder = :sortOrder WHERE id = :playlistId")
    suspend fun updateSortOrder(playlistId: Long, sortOrder: PlaylistVideoActivity.SortType)

    // Get the sort order for a specific playlist
    @Query("SELECT sortOrder FROM playlists WHERE id = :playlistId")
    suspend fun getSortOrder(playlistId: Long): PlaylistVideoActivity.SortType


}