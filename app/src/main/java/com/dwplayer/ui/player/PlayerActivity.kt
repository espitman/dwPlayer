@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.dwplayer.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.dwplayer.core.player.DwVideoPlayer
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.daos.PlaylistDao
import com.dwplayer.data.daos.SmbShareDao
import com.dwplayer.data.entities.PlaylistItemEntity
import com.dwplayer.ui.theme.DwPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    @Inject lateinit var videoPlayer: DwVideoPlayer
    @Inject lateinit var smbShareDao: SmbShareDao
    @Inject lateinit var playlistDao: PlaylistDao
    @Inject lateinit var downloadTaskDao: DownloadTaskDao

    private var hideControlsJob: Job? = null
    private var isControlsVisible by mutableStateOf(true)

    private var playlistId: String? = null
    private var currentItemId by mutableStateOf<String?>(null)
    private var playlistItems by mutableStateOf<List<PlaylistItemEntity>>(emptyList())
    private var autoNextDismissed by mutableStateOf(false)
    private var hasEndedTransitioned by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive Fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val mediaUri = intent.getStringExtra("MEDIA_URI") ?: ""
        val mediaTitle = intent.getStringExtra("MEDIA_TITLE") ?: "Movie Playback"
        val isSmb = intent.getBooleanExtra("IS_SMB", false)
        val smbShareId = intent.getStringExtra("SMB_SHARE_ID")
        val smbFilePath = intent.getStringExtra("SMB_FILE_PATH")

        playlistId = intent.getStringExtra("PLAYLIST_ID")
        currentItemId = intent.getStringExtra("PLAYLIST_ITEM_ID")

        if (!playlistId.isNullOrBlank()) {
            lifecycleScope.launch {
                val pwi = playlistDao.getPlaylistWithItemsOnce(playlistId!!)
                if (pwi != null) {
                    val sorted = pwi.sortedItems
                    playlistItems = sorted
                    if (currentItemId.isNullOrBlank() && sorted.isNotEmpty()) {
                        currentItemId = sorted.first().id
                    }
                }
            }
        }

        if (mediaUri.isNotBlank()) {
            lifecycleScope.launch {
                val share = smbShareId?.let { smbShareDao.getShareById(it) }
                videoPlayer.prepareAndPlay(
                    mediaUri = mediaUri,
                    title = mediaTitle,
                    isSmb = isSmb,
                    smbShare = share,
                    smbFilePath = smbFilePath
                )
            }
        }

        resetControlsTimeout()

        setContent {
            val uiState by videoPlayer.uiState.collectAsState()

            val currentIndex = remember(playlistItems, currentItemId) {
                playlistItems.indexOfFirst { it.id == currentItemId }
            }
            val previousItem = remember(playlistItems, currentIndex) {
                if (currentIndex > 0) playlistItems[currentIndex - 1] else null
            }
            val nextItem = remember(playlistItems, currentIndex) {
                if (currentIndex in 0 until playlistItems.size - 1) playlistItems[currentIndex + 1] else null
            }

            val remainingSec = remember(uiState.durationMs, uiState.currentPositionMs) {
                if (uiState.durationMs > 0) {
                    ((uiState.durationMs - uiState.currentPositionMs) / 1000).coerceAtLeast(0L)
                } else 0L
            }

            val showAutoNextOverlay = remember(nextItem, remainingSec, autoNextDismissed, uiState.durationMs) {
                nextItem != null && uiState.durationMs > 30_000 && remainingSec in 1..30 && !autoNextDismissed
            }

            // Auto-advance on playback completed
            LaunchedEffect(uiState.isEnded, nextItem) {
                if (uiState.isEnded && nextItem != null && !hasEndedTransitioned) {
                    hasEndedTransitioned = true
                    playPlaylistItem(nextItem)
                }
            }

            DwPlayerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // ExoPlayer Surface
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = videoPlayer.exoPlayer
                                useController = false // Custom Compose OSD
                                resizeMode = uiState.resizeMode.value
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.resizeMode = uiState.resizeMode.value
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Compose OSD Controls
                    PlayerOverlayControls(
                        uiState = uiState,
                        isVisible = isControlsVisible,
                        hasPreviousEpisode = previousItem != null,
                        hasNextEpisode = nextItem != null,
                        onPlayPreviousEpisode = {
                            previousItem?.let { playPlaylistItem(it) }
                            resetControlsTimeout()
                        },
                        onPlayNextEpisode = {
                            nextItem?.let { playPlaylistItem(it) }
                            resetControlsTimeout()
                        },
                        showAutoNextOverlay = showAutoNextOverlay,
                        autoNextTitle = nextItem?.title ?: "",
                        autoNextSeconds = remainingSec,
                        onDismissAutoNext = {
                            autoNextDismissed = true
                        },
                        onTogglePlayPause = {
                            videoPlayer.togglePlayPause()
                            resetControlsTimeout()
                        },
                        onSeekForward = {
                            videoPlayer.seekRelative(10_000)
                            resetControlsTimeout()
                        },
                        onSeekBackward = {
                            videoPlayer.seekRelative(-10_000)
                            resetControlsTimeout()
                        },
                        onSelectAudioTrack = { videoPlayer.selectAudioTrack(it) },
                        onSelectSubtitleTrack = { videoPlayer.selectSubtitleTrack(it) },
                        onSelectResizeMode = { videoPlayer.setResizeMode(it) },
                        onSelectPlaybackSpeed = { videoPlayer.setPlaybackSpeed(it) },
                        onClosePlayer = { finish() },
                        onDismissControls = {
                            hideControlsJob?.cancel()
                            isControlsVisible = false
                        }
                    )
                }
            }
        }
    }

    private fun playPlaylistItem(item: PlaylistItemEntity) {
        currentItemId = item.id
        autoNextDismissed = false
        hasEndedTransitioned = false
        lifecycleScope.launch {
            var targetUri = item.mediaUri
            if (!item.downloadTaskId.isNullOrBlank()) {
                val task = downloadTaskDao.getTaskById(item.downloadTaskId)
                if (task != null) {
                    targetUri = File(task.targetFolder, task.fileName).absolutePath
                }
            }
            videoPlayer.prepareAndPlay(
                mediaUri = targetUri,
                title = item.title,
                isSmb = false,
                smbShare = null,
                smbFilePath = null
            )
        }
    }

    private fun resetControlsTimeout() {
        isControlsVisible = true
        hideControlsJob?.cancel()
        hideControlsJob = lifecycleScope.launch {
            delay(5000)
            isControlsVisible = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (isControlsVisible) {
                    hideControlsJob?.cancel()
                    isControlsVisible = false
                    true
                } else {
                    finish()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (!isControlsVisible) {
                    resetControlsTimeout()
                    true
                } else {
                    resetControlsTimeout()
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (!videoPlayer.exoPlayer.isPlaying) videoPlayer.exoPlayer.play()
                resetControlsTimeout()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (videoPlayer.exoPlayer.isPlaying) videoPlayer.exoPlayer.pause()
                resetControlsTimeout()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!isControlsVisible) {
                    videoPlayer.seekRelative(10_000)
                    true
                } else {
                    resetControlsTimeout()
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isControlsVisible) {
                    videoPlayer.seekRelative(-10_000)
                    true
                } else {
                    resetControlsTimeout()
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!isControlsVisible) {
                    resetControlsTimeout()
                    true
                } else {
                    resetControlsTimeout()
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> {
                resetControlsTimeout()
                super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onDestroy() {
        videoPlayer.release()
        super.onDestroy()
    }
}
