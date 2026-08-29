package com.dwplayer.data.daos

import androidx.room.*
import com.dwplayer.data.entities.WebDavServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavServerDao {
    @Query("SELECT * FROM webdav_servers ORDER BY createdAt DESC")
    fun getAllServersFlow(): Flow<List<WebDavServerEntity>>

    @Query("SELECT * FROM webdav_servers ORDER BY createdAt DESC")
    suspend fun getAllServers(): List<WebDavServerEntity>

    @Query("SELECT * FROM webdav_servers WHERE id = :id")
    suspend fun getServerById(id: String): WebDavServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: WebDavServerEntity)

    @Query("DELETE FROM webdav_servers WHERE id = :id")
    suspend fun deleteServer(id: String)
}
