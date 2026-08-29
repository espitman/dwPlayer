package com.dwplayer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val mediaUri: String,
    val title: String,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isSmb: Boolean = false
)
