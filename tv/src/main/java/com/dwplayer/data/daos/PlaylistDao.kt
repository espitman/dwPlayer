package com.dwplayer.data.daos

import androidx.room.*
import com.dwplayer.data.entities.PlaylistEntity
import com.dwplayer.data.entities.PlaylistItemEntity
import com.dwplayer.data.entities.PlaylistWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylistsWithItems(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithItems(playlistId: String): Flow<PlaylistWithItems?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithItemsOnce(playlistId: String): PlaylistWithItems?

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getItemsForPlaylist(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getItemsForPlaylistOnce(playlistId: String): List<PlaylistItemEntity>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getItemCount(playlistId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Update
    suspend fun updatePlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deletePlaylistItem(itemId: String)

    @Query("UPDATE playlist_items SET orderIndex = :newOrder WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: String, newOrder: Int)

    @Transaction
    suspend fun reorderItems(orderedItemIds: List<String>) {
        orderedItemIds.forEachIndexed { index, id ->
            updateItemOrder(id, index)
        }
    }
}
