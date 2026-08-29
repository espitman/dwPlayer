package com.dwplayer.data.daos

import androidx.room.*
import com.dwplayer.data.entities.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaUri = :uri LIMIT 1")
    suspend fun getHistory(uri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE mediaUri = :uri")
    suspend fun deleteHistory(uri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
