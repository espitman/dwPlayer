package com.dwplayer.phone.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dwplayer.phone.core.discovery.NsdServerAdvertiser
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
    val mediaList: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    application: Application,
    private val httpServer: PhoneHttpServer,
    private val nsdAdvertiser: NsdServerAdvertiser,
    private val mediaScanner: PhoneMediaScanner
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    init {
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

        viewModelScope.launch {
            val qr = if (running) generateQrCode(url) else null
            _uiState.value = _uiState.value.copy(
                isServerRunning = running,
                serverUrl = url,
                qrBitmap = qr
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
