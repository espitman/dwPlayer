package com.dwplayer.data.daos

import androidx.room.*
import com.dwplayer.data.entities.SmbShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmbShareDao {
    @Query("SELECT * FROM smb_shares ORDER BY createdAt DESC")
    fun getAllShares(): Flow<List<SmbShareEntity>>

    @Query("SELECT * FROM smb_shares WHERE id = :id LIMIT 1")
    suspend fun getShareById(id: String): SmbShareEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: SmbShareEntity)

    @Delete
    suspend fun deleteShare(share: SmbShareEntity)

    @Query("DELETE FROM smb_shares WHERE id = :id")
    suspend fun deleteShareById(id: String)
}
