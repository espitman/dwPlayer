package com.dwplayer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "smb_shares")
data class SmbShareEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val host: String,
    val shareName: String,
    val username: String? = null,
    val password: String? = null,
    val domain: String? = null,
    val port: Int = 445,
    val createdAt: Long = System.currentTimeMillis()
)
