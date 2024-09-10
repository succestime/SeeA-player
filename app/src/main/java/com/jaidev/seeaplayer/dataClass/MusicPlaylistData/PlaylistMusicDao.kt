package com.jaidev.seeaplayer.dataClass.MusicPlaylistData

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jaidev.seeaplayer.VideoPlaylistFunctionality.PlaylistVideoActivity

@Dao
interface PlaylistMusicDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistMusicEntity): Long

    @Query("SELECT * FROM musicPlaylists")
    suspend fun getAllPlaylists(): List<PlaylistMusicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMusic(musicEntity: MusicEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM music WHERE musicid = :musicId)")
    suspend fun musicExists(musicId: String): Boolean

    @Insert
    suspend fun insertPlaylistMusicCrossRef(crossRef: PlaylistMusicCrossRef)

    @Insert
    suspend fun insertPlaylistMusicCrossRef(crossRefs: List<PlaylistMusicCrossRef>)


    @Transaction
    @Query("SELECT * FROM musicPlaylists WHERE musicid = :playlistMusicId")
    suspend fun getPlaylistWithMusic(playlistMusicId: Long): PlaylistWithMusics



    @Query("SELECT COUNT(*) FROM music WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)")
    suspend fun getMusicCountForPlaylist(playlistMusicId: Long): Int


    @Query("SELECT SUM(duration) FROM music WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)")
    suspend fun getTotalDurationForPlaylist(playlistMusicId: Long): Long

    @Transaction
    suspend fun getFirstMusicImageUri(playlistMusicId: Long, sortOrder: PlaylistVideoActivity.SortType): String? {
        return when (sortOrder) {
            PlaylistVideoActivity.SortType.TITLE_ASC -> getFirstMusicImageUriByTitleAsc(playlistMusicId)
            PlaylistVideoActivity.SortType.TITLE_DESC -> getFirstMusicImageUriByTitleDesc(playlistMusicId)
            PlaylistVideoActivity.SortType.DURATION_ASC -> getFirstMusicImageUriByDurationAsc(playlistMusicId)
            PlaylistVideoActivity.SortType.DURATION_DESC -> getFirstMusicImageUriByDurationDesc(playlistMusicId)
            PlaylistVideoActivity.SortType.DATE_OLDEST -> getFirstMusicImageUriByDateAddedAsc(playlistMusicId)
            PlaylistVideoActivity.SortType.DATE_NEWEST -> getFirstMusicImageUriByDateAddedDesc(playlistMusicId)
            PlaylistVideoActivity.SortType.SIZE_SMALLEST -> getFirstMusicImageUriBySizeAsc(playlistMusicId)
            PlaylistVideoActivity.SortType.SIZE_LARGEST -> getFirstMusicImageUriBySizeDesc(playlistMusicId)
        }
    }
    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.title COLLATE NOCASE ASC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByTitleAsc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.title COLLATE NOCASE DESC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByTitleDesc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.duration ASC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByDurationAsc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.duration DESC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByDurationDesc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.dateAdded ASC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByDateAddedAsc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.dateAdded DESC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriByDateAddedDesc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.size ASC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriBySizeAsc(playlistMusicId: Long): String?

    @Query("""
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
        ORDER BY v.size DESC
        LIMIT 1
    """)
    suspend fun getFirstMusicImageUriBySizeDesc(playlistMusicId: Long): String?

    @Query("UPDATE musicPlaylists SET name = :newName WHERE musicid = :playlistMusicId")
    suspend fun updatePlaylistName(playlistMusicId: Long, newName: String)

    @Query("DELETE FROM musicPlaylists  WHERE musicid = :playlistMusicId")
    suspend fun deletePlaylist(playlistMusicId: Long) // Add this method to delete a playlist by ID


    @Query(
        """
        SELECT v.artUri 
        FROM music v
        JOIN PlaylistMusicCrossRef pv ON v.musicid = pv.musicId
        WHERE pv.playlistMusicId = :playlistMusicId
    """
    )
    suspend fun getMusicForPlaylist(playlistMusicId: Long): List<String>

    @Query("DELETE FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId AND musicId = :musicId")
    suspend fun deleteMusicFromPlaylist(playlistMusicId: Long, musicId: String)


    @Query("DELETE FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId")
    suspend fun deleteAllMusicFromPlaylist(playlistMusicId: Long)

    @Query("SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId")
    suspend fun getMusicIdsForPlaylist(playlistMusicId: Long): List<String>

    @Query("SELECT COUNT(*) > 0 FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId AND musicId = :musicId")
    suspend fun isMusicInPlaylist(playlistMusicId: Long, musicId: String): Boolean

    // Sort by title A-Z
    // Sort by title A-Z
    @Query(
        """
    SELECT * FROM music
    WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
    ORDER BY title COLLATE NOCASE ASC
"""
    )
    suspend fun getMusicSortedByTitleAsc(playlistMusicId: Long): List<MusicEntity>

    // Sort by title Z-A
    @Query(
        """
    SELECT * FROM music
    WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
    ORDER BY title COLLATE NOCASE DESC
"""
    )
    suspend fun getMusicSortedByTitleDesc(playlistMusicId: Long): List<MusicEntity>

    // Sort by duration longest first
    @Query(
        """
        SELECT * FROM music
        WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
        ORDER BY duration DESC
    """
    )
    suspend fun getMusicSortedByDurationDesc(playlistMusicId: Long): List<MusicEntity>


    // Sort by duration ascending (shortest duration first)
    @Query(
        """
    SELECT * FROM music
    WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
    ORDER BY duration ASC
"""
    )
    suspend fun getMusicSortedByDurationAsc(playlistMusicId: Long): List<MusicEntity>


    // Sort by newest video first
    @Query(
        """
        SELECT * FROM music
        WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
        ORDER BY dateAdded DESC
    """
    )
    suspend fun getMusicSortedByNewest(playlistMusicId: Long): List<MusicEntity>


    // Sort by oldest video first
    @Query(
        """
        SELECT * FROM music
        WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
        ORDER BY dateAdded ASC
    """
    )
    suspend fun getMusicSortedByOldest(playlistMusicId: Long): List<MusicEntity>

    // Sort by size largest first
    @Query(
        """
        SELECT * FROM music
        WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
        ORDER BY size DESC
    """
    )
    suspend fun getMusicSortedBySizeDesc(playlistMusicId: Long): List<MusicEntity>

    // Sort by size smallest first
    @Query(
        """
        SELECT * FROM music
        WHERE musicid IN (SELECT musicId FROM PlaylistMusicCrossRef WHERE playlistMusicId = :playlistMusicId)
        ORDER BY size ASC
    """
    )
    suspend fun getMusicSortedBySizeAsc(playlistMusicId: Long): List<MusicEntity>

    @Query("UPDATE musicPlaylists SET sortOrder = :sortOrder WHERE musicid = :playlistMusicId")
    suspend fun updateSortOrder(playlistMusicId: Long, sortOrder: PlaylistVideoActivity.SortType)

    // Get the sort order for a specific playlist
    @Query("SELECT sortOrder FROM musicPlaylists WHERE musicid = :playlistMusicId")
    suspend fun getSortOrder(playlistMusicId: Long): PlaylistVideoActivity.SortType

    @Query("SELECT * FROM music WHERE path = :path LIMIT 1")
    suspend fun getMusicByPath(path: String): MusicEntity?

    @Query("DELETE FROM music WHERE musicid = :musicId")
    suspend fun deleteMusic(musicId: String)
    @Query("SELECT * FROM music")
    suspend fun getAllMusic(): List<MusicEntity>

    @Query("SELECT * FROM music WHERE musicid = :musicId LIMIT 1")
    suspend fun getMusicById(musicId: String): MusicEntity?

}