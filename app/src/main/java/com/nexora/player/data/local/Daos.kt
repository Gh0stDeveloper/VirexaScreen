package com.nexora.player.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteMediaEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId AND mediaKind = :mediaKind)")
    suspend fun isFavorite(mediaId: Long, mediaKind: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteMediaEntity): Long

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId AND mediaKind = :mediaKind")
    suspend fun delete(mediaId: Long, mediaKind: String)
}

@Dao
interface PlaylistsDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC, addedAt ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(entity: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert
    suspend fun insertPlaylistItem(entity: PlaylistItemEntity): Long

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun nextOrderIndex(playlistId: Long): Int

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItemsForPlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deletePlaylistItem(itemId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 200")
    fun observeRecent(): Flow<List<PlaybackHistoryEntity>>

    @Insert
    suspend fun insert(entity: PlaybackHistoryEntity): Long

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getByMediaId(mediaId: Long): LyricsEntity?

    @Query("SELECT * FROM lyrics WHERE mediaId = :mediaId LIMIT 1")
    fun observeByMediaId(mediaId: Long): Flow<LyricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Long)
}
