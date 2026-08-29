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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PhoneUiState(
    val isServerRunning: Boolean = false,
    val serverUrl: String = "",
    val qrBitmap: Bitmap? = null,
    val selectedFolderName: String = "No folder selected",
    val hasSelectedFolder: Boolean = false,
    val mediaList: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    application: Application,
    private val httpServer: PhoneHttpServer,
    private val nsdAdvertiser: NsdServerAdvertiser,
    private val mediaScanner: PhoneMediaScanner,
    private val folderPreferences: FolderPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    init {
        refreshState()
        loadMedia()
    }

    fun onFolderSelected(uri: Uri, displayName: String) {
        val app = getApplication<Application>()
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            app.contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        folderPreferences.saveFolder(uri, displayName)
        refreshState()
        loadMedia()
    }

    fun toggleServer(enable: Boolean) {
        val app = getApplication<Application>()
        val intent = Intent(app, PhoneServerService::class.java).apply {
            action = if (enable) PhoneServerService.ACTION_START else PhoneServerService.ACTION_STOP
        }

        if (enable) {
            ContextCompat.startForegroundService(app, intent)
        } else {
            app.startService(intent)
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            refreshState()
        }
    }

    fun refreshState() {
        val running = httpServer.isRunning
        val ip = nsdAdvertiser.getLocalIpAddress()
        val url = "http://$ip:8085"
        val folderName = folderPreferences.getFolderName()
        val hasFolder = folderPreferences.hasSelectedFolder()

        viewModelScope.launch {
            val qr = if (running) generateQrCode(url) else null
            _uiState.value = _uiState.value.copy(
                isServerRunning = running,
                serverUrl = url,
                qrBitmap = qr,
                selectedFolderName = folderName,
                hasSelectedFolder = hasFolder
            )
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMedia = true)
            val videos = mediaScanner.getVideos()
            _uiState.value = _uiState.value.copy(
                mediaList = videos,
                isLoadingMedia = false
            )
        }
    }

    private suspend fun generateQrCode(content: String): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
