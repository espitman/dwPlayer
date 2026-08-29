package com.dwplayer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "webdav_servers")
data class WebDavServerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null,
    val isAutoDiscovered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
