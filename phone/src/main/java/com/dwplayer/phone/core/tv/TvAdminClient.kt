package com.dwplayer.phone.core.tv

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TvStorageInfo(val freeSpace: String = "—", val totalSpace: String = "—", val usedPercent: Int = 0, val path: String = "")

@Serializable
data class TvTaskSummary(val total: Int = 0, val active: Int = 0, val paused: Int = 0, val failed: Int = 0, val completed: Int = 0)

@Serializable
data class TvDownloadTask(
    val id: String,
    val url: String = "",
    val targetFolder: String = "",
    val fileName: String,
    val status: String,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: String = "",
    val timeRemaining: String = "",
    val etaTimestamp: Long? = null,
    val createdAt: Long = 0,
    val completedAt: Long? = null
)

@Serializable
data class TvTaskResponse(val tasks: List<TvDownloadTask> = emptyList(), val summary: TvTaskSummary = TvTaskSummary())

@Serializable
data class TvPlaylistItem(
    val id: String,
    val playlistId: String,
    val title: String,
    val mediaUri: String = "",
    val downloadTaskId: String? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = 0
)

@Serializable
data class TvPlaylist(
    val id: String,
    val name: String,
    val itemCount: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val items: List<TvPlaylistItem> = emptyList()
)

@Serializable
data class TvWebDavServer(
    val id: String,
    val name: String,
    val serverUrl: String,
    val username: String? = null,
    val password: String? = null,
    val isAutoDiscovered: Boolean = false,
    val createdAt: Long = 0
)

@Serializable
data class TvSmbShare(
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

@Serializable
data class TvDiscoveredServer(
    val serviceName: String,
    val serviceType: String = "",
    val host: String,
    val port: Int,
    val url: String,
    val deviceType: String = ""
)

@Serializable
data class TvRemoteStatus(
    val playerActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

@Serializable
data class TvArchiveFile(
    val name: String,
    val path: String,
    val sizeBytes: Long = 0,
    val sizeFormatted: String = "",
    val lastModified: Long = 0,
    val extension: String = "",
    val durationMs: Long = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
)

@Serializable private data class DownloadBody(val url: String, val fileName: String? = null, val playlistId: String? = null)
@Serializable private data class PlaylistBody(val name: String)
@Serializable private data class WebDavBody(val name: String, val serverUrl: String, val username: String? = null, val password: String? = null)
@Serializable private data class PlayUrlBody(val url: String, val title: String)

@Singleton
class TvAdminClient @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("dwshare_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    var baseUrl: String
        get() = prefs.getString(KEY_TV_URL, DEFAULT_TV_URL) ?: DEFAULT_TV_URL
        private set(value) { prefs.edit().putString(KEY_TV_URL, normalize(value)).apply() }

    fun saveBaseUrl(value: String) { baseUrl = value }

    suspend fun storage() = get<TvStorageInfo>("/api/storage/info")
    suspend fun downloads() = get<TvTaskResponse>("/api/status/live")
    suspend fun playlists() = get<List<TvPlaylist>>("/api/playlists")
    suspend fun webDavServers() = get<List<TvWebDavServer>>("/api/webdav/servers")
    suspend fun smbShares() = get<List<TvSmbShare>>("/api/smb/shares")
    suspend fun discoveredServers() = get<List<TvDiscoveredServer>>("/api/discovery/servers")
    suspend fun remoteStatus() = get<TvRemoteStatus>("/api/remote/status")
    suspend fun archive() = get<List<TvArchiveFile>>("/api/archive")

    suspend fun sendDownload(url: String, fileName: String?, playlistId: String?) =
        post("/api/download", json.encodeToString(DownloadBody(url, fileName?.ifBlank { null }, playlistId)))

    suspend fun createPlaylist(name: String) = post("/api/playlists", json.encodeToString(PlaylistBody(name)))
    suspend fun deletePlaylist(id: String) = delete("/api/playlists/$id")
    suspend fun playPlaylist(id: String) = post("/api/playlists/$id/play", "{}")

    suspend fun saveWebDav(name: String, url: String, username: String?, password: String?) =
        post("/api/webdav/servers", json.encodeToString(WebDavBody(name, url, username?.ifBlank { null }, password?.ifBlank { null })))

    suspend fun deleteWebDav(id: String) = delete("/api/webdav/servers/$id")

    suspend fun saveSmb(name: String, host: String, shareName: String, username: String?, password: String?, port: Int) =
        post("/api/smb/shares", json.encodeToString(TvSmbShare(UUID.randomUUID().toString(), name, host, shareName, username?.ifBlank { null }, password?.ifBlank { null }, port = port)))

    suspend fun deleteSmb(id: String) = delete("/api/smb/shares/$id")
    suspend fun remote(command: String) = post("/api/remote/$command", "{}")
    suspend fun playUrl(url: String, title: String) = post("/api/play-url", json.encodeToString(PlayUrlBody(url, title)))

    private suspend inline fun <reified T> get(path: String): T = request("GET", path).let { json.decodeFromString(it) }
    private suspend fun post(path: String, body: String) = request("POST", path, body)
    private suspend fun delete(path: String) = request("DELETE", path)

    private suspend fun request(method: String, path: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val connection = (URL(normalize(baseUrl) + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 2_500
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toByteArray()) }
            }
        }
        try {
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error(text.ifBlank { "TV returned HTTP $code" })
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isBlank() -> DEFAULT_TV_URL
            else -> "http://$trimmed"
        }
    }

    companion object {
        private const val KEY_TV_URL = "tv_admin_url"
        private const val DEFAULT_TV_URL = "http://10.0.2.2:8200"
    }
}
