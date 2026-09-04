package com.dwplayer.phone.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dwplayer.phone.core.discovery.NsdServerAdvertiser
import com.dwplayer.phone.core.media.FolderPreferences
import com.dwplayer.phone.core.media.MediaItem
import com.dwplayer.phone.core.media.PhoneMediaScanner
import com.dwplayer.phone.core.server.PhoneHttpServer
import com.dwplayer.phone.core.server.PhoneServerService
import com.dwplayer.phone.core.tv.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PhoneDestination { HOME, LIBRARY, SEND, PLAYLISTS, NETWORK }

data class PhoneUiState(
    val destination: PhoneDestination = PhoneDestination.HOME,
    val isServerRunning: Boolean = false,
    val serverUrl: String = "",
    val qrBitmap: Bitmap? = null,
    val selectedFolderName: String = "No folder selected",
    val hasSelectedFolder: Boolean = false,
    val mediaList: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val tvUrl: String = "",
    val tvConnected: Boolean = false,
    val isRefreshingTv: Boolean = false,
    val storage: TvStorageInfo? = null,
    val downloads: TvTaskResponse = TvTaskResponse(),
    val playlists: List<TvPlaylist> = emptyList(),
    val webDavServers: List<TvWebDavServer> = emptyList(),
    val smbShares: List<TvSmbShare> = emptyList(),
    val discoveredServers: List<TvDiscoveredServer> = emptyList(),
    val isScanningNetwork: Boolean = false,
    val remoteStatus: TvRemoteStatus = TvRemoteStatus(),
    val tvArchive: List<TvArchiveFile> = emptyList(),
    val message: String? = null,
    val messageId: Long = 0
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    application: Application,
    private val httpServer: PhoneHttpServer,
    private val nsdAdvertiser: NsdServerAdvertiser,
    private val mediaScanner: PhoneMediaScanner,
    private val folderPreferences: FolderPreferences,
    private val tvClient: TvAdminClient
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PhoneUiState(tvUrl = tvClient.baseUrl))
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    init {
        refreshState()
        loadMedia()
        refreshTv()
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                refreshTv(silent = true)
            }
        }
    }

    fun navigate(destination: PhoneDestination) { _uiState.value = _uiState.value.copy(destination = destination) }

    fun onFolderSelected(uri: Uri, displayName: String) {
        val app = getApplication<Application>()
        runCatching { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        folderPreferences.saveFolder(uri, displayName)
        refreshState()
        loadMedia(force = true)
        notify("Shared folder updated")
    }

    fun toggleServer(enable: Boolean) {
        if (enable && !folderPreferences.hasSelectedFolder()) {
            notify("Choose a folder first")
            return
        }
        val app = getApplication<Application>()
        val intent = Intent(app, PhoneServerService::class.java).apply {
            action = if (enable) PhoneServerService.ACTION_START else PhoneServerService.ACTION_STOP
        }
        if (enable) ContextCompat.startForegroundService(app, intent) else app.startService(intent)
        viewModelScope.launch {
            delay(650)
            refreshState()
            notify(if (enable) "Phone server started" else "Phone server stopped")
        }
    }

    fun refreshState() {
        val running = httpServer.isRunning
        val url = "http://${nsdAdvertiser.getLocalIpAddress()}:8085"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isServerRunning = running,
                serverUrl = url,
                qrBitmap = if (running) generateQrCode(url) else null,
                selectedFolderName = folderPreferences.getFolderName(),
                hasSelectedFolder = folderPreferences.hasSelectedFolder()
            )
        }
    }

    fun loadMedia(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMedia = true)
            val videos = mediaScanner.getVideos(force)
            _uiState.value = _uiState.value.copy(mediaList = videos, isLoadingMedia = false)
        }
    }

    fun connectTv(url: String) {
        tvClient.saveBaseUrl(url)
        _uiState.value = _uiState.value.copy(tvUrl = tvClient.baseUrl)
        refreshTv()
    }

    fun refreshTv(silent: Boolean = false) {
        if (_uiState.value.isRefreshingTv) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingTv = true)
            val storageResult = runCatching { tvClient.storage() }
            if (storageResult.isFailure) {
                _uiState.value = _uiState.value.copy(tvConnected = false, isRefreshingTv = false)
                if (!silent) notify("Could not reach dwPlayer TV")
                return@launch
            }
            _uiState.value = _uiState.value.copy(tvConnected = true, storage = storageResult.getOrNull())
            runCatching { tvClient.downloads() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(downloads = it) }
            runCatching { tvClient.playlists() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(playlists = it) }
            runCatching { tvClient.webDavServers() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(webDavServers = it) }
            runCatching { tvClient.smbShares() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(smbShares = it) }
            runCatching { tvClient.remoteStatus() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(remoteStatus = it) }
            runCatching { tvClient.archive() }.getOrNull()?.let { _uiState.value = _uiState.value.copy(tvArchive = it) }
            _uiState.value = _uiState.value.copy(isRefreshingTv = false)
            if (!silent) notify("Living Room TV connected")
        }
    }

    fun remote(command: String) = tvAction("Remote command sent") { tvClient.remote(command) }

    fun playPhoneMedia(media: MediaItem) {
        if (!folderPreferences.hasSelectedFolder()) { notify("Choose a folder first"); return }
        viewModelScope.launch {
            if (!httpServer.isRunning) {
                val app = getApplication<Application>()
                ContextCompat.startForegroundService(app, Intent(app, PhoneServerService::class.java).apply { action = PhoneServerService.ACTION_START })
                delay(800)
                refreshState()
            }
            val stream = "${_uiState.value.serverUrl}/api/stream/${media.id}"
            runCatching { tvClient.playUrl(stream, media.title) }
                .onSuccess { notify("Playing on Living Room TV") }
                .onFailure { notify(it.message?.take(110) ?: "Could not play on TV") }
        }
    }

    fun playTvArchive(media: TvArchiveFile) = tvAction("Playing on Living Room TV") {
        tvClient.playUrl(media.path, media.name)
    }

    fun sendDownload(url: String, fileName: String, playlistId: String?) {
        if (url.isBlank()) { notify("Enter a video URL"); return }
        tvAction("Download sent to TV", refresh = true) { tvClient.sendDownload(url.trim(), fileName.trim(), playlistId) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) { notify("Enter a playlist name"); return }
        tvAction("Playlist created", refresh = true) { tvClient.createPlaylist(name.trim()) }
    }

    fun deletePlaylist(id: String) = tvAction("Playlist deleted", refresh = true) { tvClient.deletePlaylist(id) }
    fun playPlaylist(id: String) = tvAction("Playlist started on TV") { tvClient.playPlaylist(id) }

    fun saveWebDav(name: String, url: String, username: String, password: String) {
        if (name.isBlank() || url.isBlank()) { notify("Name and server URL are required"); return }
        tvAction("WebDAV source saved to TV", refresh = true) { tvClient.saveWebDav(name.trim(), url.trim(), username, password) }
    }

    fun saveSmb(name: String, host: String, share: String, username: String, password: String, port: String) {
        if (name.isBlank() || host.isBlank() || share.isBlank()) { notify("Name, host and share are required"); return }
        tvAction("SMB source saved to TV", refresh = true) { tvClient.saveSmb(name.trim(), host.trim(), share.trim(), username, password, port.toIntOrNull() ?: 445) }
    }

    fun deleteWebDav(id: String) = tvAction("WebDAV source removed", refresh = true) { tvClient.deleteWebDav(id) }
    fun deleteSmb(id: String) = tvAction("SMB source removed", refresh = true) { tvClient.deleteSmb(id) }

    fun scanNetwork() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningNetwork = true)
            runCatching { tvClient.discoveredServers() }
                .onSuccess { _uiState.value = _uiState.value.copy(discoveredServers = it); notify("Network scan updated") }
                .onFailure { notify("TV must be connected to scan") }
            delay(650)
            _uiState.value = _uiState.value.copy(isScanningNetwork = false)
        }
    }

    private fun tvAction(success: String, refresh: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { notify(success); if (refresh) refreshTv(silent = true) }
                .onFailure { notify(it.message?.take(110) ?: "TV request failed") }
        }
    }

    private fun notify(text: String) {
        _uiState.value = _uiState.value.copy(message = text, messageId = _uiState.value.messageId + 1)
    }

    private suspend fun generateQrCode(content: String): Bitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
            Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).also { bitmap ->
                for (x in 0 until matrix.width) for (y in 0 until matrix.height) bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }.getOrNull()
    }
}
