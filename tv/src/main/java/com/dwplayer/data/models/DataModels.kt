package com.dwplayer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadProgressInfo(
    val id: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: String = "",
    val speedBytesPerSec: Long = 0L,
    val timeRemaining: String = "",
    val etaTimestamp: Long? = null
)

@Serializable
data class ApiResponse(
    val status: String,
    val message: String? = null
)

@Serializable
data class LiveTaskItem(
    val id: String,
    val url: String,
    val targetFolder: String,
    val fileName: String,
    val status: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: String,
    val timeRemaining: String,
    val etaTimestamp: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null
)

@Serializable
data class LiveTaskSummary(
    val total: Int,
    val active: Int,
    val paused: Int,
    val failed: Int,
    val completed: Int
)

@Serializable
data class LiveTaskResponse(
    val tasks: List<LiveTaskItem>,
    val summary: LiveTaskSummary
)

@Serializable
data class SmbItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L
)

@Serializable
data class SmbBrowseResponse(
    val shareId: String,
    val path: String,
    val items: List<SmbItem>
)

@Serializable
data class StorageInfo(
    val freeSpace: String,
    val totalSpace: String,
    val usedPercent: Int,
    val path: String
)

@Serializable
data class DownloadRequest(
    val url: String,
    val fileName: String? = null,
    val playlistId: String? = null
)

@Serializable
data class SmbTestRequest(
    val host: String,
    val shareName: String,
    val username: String? = null,
    val password: String? = null,
    val domain: String? = null,
    val port: Int = 445
)

@Serializable
data class SmbPlayRequest(
    val shareId: String,
    val filePath: String,
    val title: String? = null
)

@Serializable
data class LocalArchiveFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lastModified: Long,
    val extension: String,
    val durationMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
)

@Serializable
data class DeleteArchiveRequest(
    val path: String
)

@Serializable
data class CreatePlaylistRequest(
    val name: String
)

@Serializable
data class AddPlaylistItemRequest(
    val title: String,
    val mediaUri: String,
    val downloadTaskId: String? = null
)

@Serializable
data class ReorderPlaylistRequest(
    val itemIds: List<String>
)

@Serializable
data class PlaylistItemDto(
    val id: String,
    val playlistId: String,
    val title: String,
    val mediaUri: String,
    val downloadTaskId: String? = null,
    val orderIndex: Int,
    val createdAt: Long
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<PlaylistItemDto> = emptyList()
)

@Serializable
data class WebDavItem(
    val name: String,
    val path: String,
    val fullUrl: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val formattedSize: String = "",
    val lastModified: String = "",
    val isVideo: Boolean = false
)

@Serializable
data class WebDavBrowseResponse(
    val serverId: String,
    val serverName: String,
    val path: String,
    val items: List<WebDavItem>
)

@Serializable
data class AddWebDavServerRequest(
    val name: String,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class DiscoveredServerDto(
    val serviceName: String,
    val serviceType: String,
    val host: String,
    val port: Int,
    val url: String,
    val deviceType: String
)

@Serializable
data class WebDavPlayRequest(
    val serverId: String,
    val fileUrl: String,
    val title: String? = null
)

@Serializable
data class PlayUrlRequest(
    val url: String,
    val title: String? = null
)

@Serializable
data class RemotePlayerStatus(
    val playerActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
