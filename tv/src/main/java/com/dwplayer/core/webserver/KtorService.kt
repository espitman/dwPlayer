package com.dwplayer.core.webserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dwplayer.R
import com.dwplayer.core.downloader.DwDownloadManager
import com.dwplayer.core.downloader.StorageManager
import com.dwplayer.core.smb.SmbClientManager
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.daos.PlaybackHistoryDao
import com.dwplayer.data.daos.SmbShareDao
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.models.*
import com.dwplayer.ui.player.PlayerActivity
import com.dwplayer.ui.player.PlayerRemoteBridge
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.default
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class KtorService : Service() {

    companion object {
        private const val CHANNEL_ID = "dwplayer_ktor_service_channel"
        private const val NOTIFICATION_ID = 1002
        private const val SERVER_PORT = 8200
    }

    @Inject lateinit var downloadManager: DwDownloadManager
    @Inject lateinit var downloadTaskDao: DownloadTaskDao
    @Inject lateinit var smbShareDao: SmbShareDao
    @Inject lateinit var playbackHistoryDao: PlaybackHistoryDao
    @Inject lateinit var playlistDao: com.dwplayer.data.daos.PlaylistDao
    @Inject lateinit var webDavServerDao: com.dwplayer.data.daos.WebDavServerDao
    @Inject lateinit var webDavClientManager: com.dwplayer.core.webdav.WebDavClientManager
    @Inject lateinit var networkDiscoveryManager: com.dwplayer.core.discovery.NetworkDiscoveryManager
    @Inject lateinit var smbClientManager: SmbClientManager
    @Inject lateinit var storageManager: StorageManager

    private var server: NettyApplicationEngine? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startKtorServer()
        networkDiscoveryManager.startDiscovery()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "dwPlayer Web Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("dwPlayer Cinema Hub")
            .setContentText("Companion Web Server running on port $SERVER_PORT")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startKtorServer() {
        serviceScope.launch {
            try {
                server = embeddedServer(Netty, port = SERVER_PORT) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        })
                    }
                    install(CORS) {
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Post)
                        allowMethod(HttpMethod.Put)
                        allowMethod(HttpMethod.Delete)
                        allowHeader(HttpHeaders.Authorization)
                        allowHeader(HttpHeaders.ContentType)
                        anyHost()
                    }

                    routing {
                        // Static resources for web dashboard
                        staticResources("/", "web") {
                            default("index.html")
                        }

                        route("/api") {
                            get("/remote/status") {
                                val status = withContext(Dispatchers.Main) { PlayerRemoteBridge.status() }
                                call.respond(status)
                            }

                            post("/remote/{command}") {
                                val command = call.parameters["command"]
                                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "Command required"))
                                val handled = withContext(Dispatchers.Main) { PlayerRemoteBridge.send(command) }
                                if (handled) call.respond(ApiResponse("success", "Remote command sent"))
                                else call.respond(HttpStatusCode.Conflict, ApiResponse("error", "Player is not open"))
                            }

                            post("/play-url") {
                                val req = call.receive<PlayUrlRequest>()
                                if (req.url.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "URL required"))
                                val title = req.title?.ifBlank { null } ?: req.url.substringAfterLast('/').substringBefore('?').ifBlank { "Network video" }
                                startActivity(Intent(this@KtorService, PlayerActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("MEDIA_URI", req.url)
                                    putExtra("MEDIA_TITLE", title)
                                })
                                call.respond(ApiResponse("success", "Playing $title on TV"))
                            }

                            // 1. Storage info
                            get("/storage/info") {
                                val info = storageManager.getStorageInfo()
                                call.respond(info)
                            }

                            // 2. Add download task
                            post("/download") {
                                try {
                                    val req = call.receive<DownloadRequest>()
                                    if (req.url.isBlank()) {
                                        return@post call.respond(
                                            HttpStatusCode.BadRequest,
                                            ApiResponse("error", "URL is required")
                                        )
                                    }
                                    val taskId = downloadManager.enqueueDownload(req.url, req.fileName)
                                    if (!req.playlistId.isNullOrBlank()) {
                                        val count = playlistDao.getItemCount(req.playlistId)
                                        val task = downloadTaskDao.getTaskById(taskId)
                                        val title = req.fileName?.ifBlank { null }
                                            ?: req.url.substringAfterLast("/").substringBefore("?").ifBlank { "Episode ${count + 1}" }
                                        val targetFile = if (task != null) File(task.targetFolder, task.fileName).absolutePath else ""
                                        playlistDao.insertPlaylistItem(
                                            com.dwplayer.data.entities.PlaylistItemEntity(
                                                playlistId = req.playlistId,
                                                title = title,
                                                mediaUri = targetFile,
                                                downloadTaskId = taskId,
                                                orderIndex = count
                                            )
                                        )
                                    }
                                    call.respond(ApiResponse("success", "Download queued with ID $taskId"))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            // 3. Live tasks status
                            get("/status/live") {
                                val tasks = downloadTaskDao.getAllTasks().first()
                                val live = downloadManager.downloadStatus.value

                                val items = tasks.map { task ->
                                    val liveInfo = live[task.id]
                                    LiveTaskItem(
                                        id = task.id,
                                        url = task.url,
                                        targetFolder = task.targetFolder,
                                        fileName = task.fileName,
                                        status = task.status,
                                        progress = liveInfo?.progress ?: task.progress,
                                        downloadedBytes = liveInfo?.downloadedBytes ?: task.downloadedBytes,
                                        totalBytes = liveInfo?.totalBytes ?: task.totalBytes,
                                        speed = liveInfo?.speed ?: "",
                                        timeRemaining = liveInfo?.timeRemaining ?: "",
                                        etaTimestamp = liveInfo?.etaTimestamp,
                                        createdAt = task.createdAt,
                                        completedAt = task.completedAt
                                    )
                                }

                                val summary = LiveTaskSummary(
                                    total = tasks.size,
                                    active = tasks.count { it.status == "ACTIVE" || it.status == "PENDING" },
                                    paused = tasks.count { it.status == "PAUSED" },
                                    failed = tasks.count { it.status == "FAILED" },
                                    completed = tasks.count { it.status == "COMPLETED" }
                                )

                                call.respond(LiveTaskResponse(tasks = items, summary = summary))
                            }

                            // 4. Pause / Resume / Delete task
                            post("/downloads/{id}/pause") {
                                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                                downloadManager.pauseDownload(id)
                                call.respond(ApiResponse("success", "Paused $id"))
                            }

                            post("/downloads/{id}/resume") {
                                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                                downloadManager.resumeDownload(id)
                                call.respond(ApiResponse("success", "Resumed $id"))
                            }

                            delete("/downloads/{id}") {
                                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                                val deleteFile = call.request.queryParameters["deleteFile"]?.toBoolean() ?: true
                                downloadManager.deleteDownload(id, deleteFile)
                                call.respond(ApiResponse("success", "Deleted $id"))
                            }

                            // 5. Play downloaded video on TV
                            post("/downloads/{id}/play") {
                                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                                val task = downloadTaskDao.getTaskById(id)
                                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse("error", "Task not found"))

                                val file = File(task.targetFolder, task.fileName)
                                val playIntent = Intent(this@KtorService, PlayerActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("MEDIA_URI", file.absolutePath)
                                    putExtra("MEDIA_TITLE", task.fileName)
                                    putExtra("IS_LOCAL_FILE", true)
                                }
                                startActivity(playIntent)
                                call.respond(ApiResponse("success", "Playing ${task.fileName} on TV"))
                            }

                            // 6. SMB Endpoints
                            get("/smb/shares") {
                                val shares = smbShareDao.getAllShares().first()
                                call.respond(shares)
                            }

                            // 7. Local Media Archive Endpoints
                            get("/archive") {
                                val files = storageManager.getLocalArchiveFiles()
                                call.respond(files)
                            }

                            delete("/archive") {
                                try {
                                    val req = call.receive<DeleteArchiveRequest>()
                                    val ok = storageManager.deleteLocalArchiveFile(req.path)
                                    if (ok) {
                                        call.respond(ApiResponse("success", "File deleted"))
                                    } else {
                                        call.respond(HttpStatusCode.NotFound, ApiResponse("error", "File could not be deleted"))
                                    }
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            post("/smb/shares") {
                                try {
                                    val share = call.receive<SmbShareEntity>()
                                    val toSave = if (share.id.isBlank()) share.copy(id = UUID.randomUUID().toString()) else share
                                    smbShareDao.insertShare(toSave)
                                    call.respond(ApiResponse("success", "Saved share ${toSave.name}"))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.BadRequest, ApiResponse("error", e.message))
                                }
                            }

                            delete("/smb/shares/{id}") {
                                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                                smbShareDao.deleteShareById(id)
                                call.respond(ApiResponse("success", "Deleted share $id"))
                            }

                            post("/smb/test") {
                                val req = call.receive<SmbTestRequest>()
                                val result = smbClientManager.testConnection(
                                    host = req.host,
                                    shareName = req.shareName,
                                    username = req.username,
                                    password = req.password,
                                    domain = req.domain,
                                    port = req.port
                                )
                                if (result.isSuccess) {
                                    call.respond(ApiResponse("success", result.getOrNull()))
                                } else {
                                    call.respond(ApiResponse("error", result.exceptionOrNull()?.message ?: "Failed"))
                                }
                            }

                            get("/smb/browse") {
                                val shareId = call.request.queryParameters["shareId"]
                                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "shareId required"))
                                val path = call.request.queryParameters["path"] ?: ""

                                val share = smbShareDao.getShareById(shareId)
                                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse("error", "Share not found"))

                                val items = smbClientManager.listDirectory(share, path)
                                call.respond(SmbBrowseResponse(shareId = shareId, path = path, items = items))
                            }

                            post("/smb/play") {
                                val req = call.receive<SmbPlayRequest>()
                                val share = smbShareDao.getShareById(req.shareId)
                                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse("error", "Share not found"))

                                val playIntent = Intent(this@KtorService, PlayerActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("MEDIA_URI", "smb://${share.id}/${req.filePath}")
                                    putExtra("SMB_SHARE_ID", share.id)
                                    putExtra("SMB_FILE_PATH", req.filePath)
                                    putExtra("MEDIA_TITLE", req.title ?: req.filePath.substringAfterLast("/"))
                                    putExtra("IS_SMB", true)
                                }
                                startActivity(playIntent)
                                call.respond(ApiResponse("success", "Playing SMB file on TV"))
                            }

                            // 8. Playlist & Series Endpoints
                            get("/playlists") {
                                val playlistsWithItems = playlistDao.getAllPlaylistsWithItems().first()
                                val dtos = playlistsWithItems.map { pwi ->
                                    PlaylistDto(
                                        id = pwi.playlist.id,
                                        name = pwi.playlist.name,
                                        itemCount = pwi.items.size,
                                        createdAt = pwi.playlist.createdAt,
                                        updatedAt = pwi.playlist.updatedAt,
                                        items = pwi.sortedItems.map { item ->
                                            PlaylistItemDto(
                                                id = item.id,
                                                playlistId = item.playlistId,
                                                title = item.title,
                                                mediaUri = item.mediaUri,
                                                downloadTaskId = item.downloadTaskId,
                                                orderIndex = item.orderIndex,
                                                createdAt = item.createdAt
                                            )
                                        }
                                    )
                                }
                                call.respond(dtos)
                            }

                            post("/playlists") {
                                try {
                                    val req = call.receive<CreatePlaylistRequest>()
                                    if (req.name.isBlank()) {
                                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "Playlist name is required"))
                                    }
                                    val playlist = com.dwplayer.data.entities.PlaylistEntity(name = req.name.trim())
                                    playlistDao.insertPlaylist(playlist)
                                    call.respond(ApiResponse("success", playlist.id))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            delete("/playlists/{id}") {
                                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                                playlistDao.deletePlaylist(id)
                                call.respond(ApiResponse("success", "Playlist deleted"))
                            }

                            post("/playlists/{id}/items") {
                                try {
                                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                                    val req = call.receive<AddPlaylistItemRequest>()
                                    val count = playlistDao.getItemCount(id)
                                    val item = com.dwplayer.data.entities.PlaylistItemEntity(
                                        playlistId = id,
                                        title = req.title.ifBlank { "Episode ${count + 1}" },
                                        mediaUri = req.mediaUri,
                                        downloadTaskId = req.downloadTaskId,
                                        orderIndex = count
                                    )
                                    playlistDao.insertPlaylistItem(item)
                                    call.respond(ApiResponse("success", item.id))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            put("/playlists/{id}/reorder") {
                                try {
                                    val req = call.receive<ReorderPlaylistRequest>()
                                    playlistDao.reorderItems(req.itemIds)
                                    call.respond(ApiResponse("success", "Playlist reordered"))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            delete("/playlists/{id}/items/{itemId}") {
                                val itemId = call.parameters["itemId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                                playlistDao.deletePlaylistItem(itemId)
                                call.respond(ApiResponse("success", "Item deleted"))
                            }

                            post("/playlists/{id}/play") {
                                val playlistId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                                val startItemId = call.request.queryParameters["itemId"]
                                val pwi = playlistDao.getPlaylistWithItemsOnce(playlistId)
                                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse("error", "Playlist not found"))
                                val items = pwi.sortedItems
                                if (items.isEmpty()) {
                                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "Playlist is empty"))
                                }
                                val targetItem = if (startItemId != null) items.find { it.id == startItemId } ?: items.first() else items.first()

                                var finalUri = targetItem.mediaUri
                                if (targetItem.downloadTaskId != null) {
                                    val task = downloadTaskDao.getTaskById(targetItem.downloadTaskId)
                                    if (task != null) {
                                        finalUri = File(task.targetFolder, task.fileName).absolutePath
                                    }
                                }

                                val playIntent = Intent(this@KtorService, PlayerActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("MEDIA_URI", finalUri)
                                    putExtra("MEDIA_TITLE", targetItem.title)
                                    putExtra("PLAYLIST_ID", playlistId)
                                    putExtra("PLAYLIST_ITEM_ID", targetItem.id)
                                }
                                startActivity(playIntent)
                                call.respond(ApiResponse("success", "Playing ${targetItem.title} on TV"))
                            }

                            // WEBDAV & DISCOVERY ENDPOINTS
                            get("/discovery/servers") {
                                val discovered = networkDiscoveryManager.discoveredServers.value
                                call.respond(discovered)
                            }

                            get("/webdav/servers") {
                                val servers = webDavServerDao.getAllServers()
                                call.respond(servers)
                            }

                            post("/webdav/servers") {
                                try {
                                    val req = call.receive<AddWebDavServerRequest>()
                                    if (req.name.isBlank() || req.serverUrl.isBlank()) {
                                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", "Name and Server URL are required"))
                                    }

                                    val testResult = webDavClientManager.testConnection(req.serverUrl, req.username, req.password)
                                    if (testResult.isFailure) {
                                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse("error", testResult.exceptionOrNull()?.message ?: "WebDAV connection failed"))
                                    }

                                    val serverEntity = com.dwplayer.data.entities.WebDavServerEntity(
                                        name = req.name.trim(),
                                        serverUrl = req.serverUrl.trim(),
                                        username = req.username?.takeIf { it.isNotBlank() },
                                        password = req.password?.takeIf { it.isNotBlank() }
                                    )
                                    webDavServerDao.insertServer(serverEntity)
                                    call.respond(ApiResponse("success", serverEntity.id))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }

                            delete("/webdav/servers/{id}") {
                                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                                webDavServerDao.deleteServer(id)
                                call.respond(ApiResponse("success", "WebDAV server removed"))
                            }

                            get("/webdav/servers/{id}/files") {
                                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                                val server = webDavServerDao.getServerById(id)
                                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse("error", "Server not found"))

                                val path = call.request.queryParameters["path"] ?: ""
                                val listResult = webDavClientManager.listFiles(
                                    serverUrl = server.serverUrl,
                                    subPath = path,
                                    username = server.username,
                                    password = server.password
                                )

                                if (listResult.isSuccess) {
                                    val items = listResult.getOrDefault(emptyList())
                                    call.respond(
                                        WebDavBrowseResponse(
                                            serverId = server.id,
                                            serverName = server.name,
                                            path = path,
                                            items = items
                                        )
                                    )
                                } else {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        ApiResponse("error", listResult.exceptionOrNull()?.message ?: "Failed to list WebDAV files")
                                    )
                                }
                            }

                            post("/webdav/play") {
                                try {
                                    val req = call.receive<WebDavPlayRequest>()
                                    val server = webDavServerDao.getServerById(req.serverId)
                                    val title = req.title ?: req.fileUrl.substringAfterLast('/')

                                    val authHeader = if (!server?.username.isNullOrBlank()) {
                                        val credentials = "${server?.username}:${server?.password ?: ""}"
                                        val encoded = android.util.Base64.encodeToString(credentials.toByteArray(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                                        "Basic $encoded"
                                    } else null

                                    val playIntent = Intent(this@KtorService, PlayerActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        putExtra("MEDIA_URI", req.fileUrl)
                                        putExtra("MEDIA_TITLE", title)
                                        if (authHeader != null) {
                                            putExtra("AUTH_HEADER", authHeader)
                                        }
                                    }
                                    startActivity(playIntent)
                                    call.respond(ApiResponse("success", "Playing $title on TV"))
                                } catch (e: Exception) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse("error", e.message))
                                }
                            }
                        }
                    }
                }
                server?.start(wait = true)
            } catch (e: Exception) {
                Log.e("KtorService", "Ktor server start failed", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        networkDiscoveryManager.stopDiscovery()
        server?.stop(1000, 2000)
        serviceJob.cancel()
        super.onDestroy()
    }
}
