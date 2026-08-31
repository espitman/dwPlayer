@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.dwplayer.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
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
    private var quickSeekFeedbackJob: Job? = null
    private var quickSeekSeconds by mutableStateOf<Int?>(null)
    private var quickSeekEventId by mutableLongStateOf(0L)

    private var playlistId: String? = null
    private var currentItemId by mutableStateOf<String?>(null)
    private var playlistItems by mutableStateOf<List<PlaylistItemEntity>>(emptyList())
    private var autoNextDismissed by mutableStateOf(false)
    private var hasEndedTransitioned by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screensaver / sleep during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
        val authHeader = intent.getStringExtra("AUTH_HEADER")

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
                videoPlayer.playMedia(
                    mediaUri = mediaUri,
                    title = mediaTitle,
                    isSmb = isSmb,
                    smbShare = share,
                    smbFilePath = smbFilePath,
                    authHeader = authHeader
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

                            // Apply Dynamic Subtitle Customization (Vazirmatn, Colors, Background, Size, Position)
                            val subSettings = uiState.subtitleSettings
                            val subtitleView = view.subtitleView
                            if (subtitleView != null) {
                                val typeface = subSettings.font.fontResId?.let {
                                    androidx.core.content.res.ResourcesCompat.getFont(view.context, it)
                                }
                                val captionStyle = androidx.media3.ui.CaptionStyleCompat(
                                    subSettings.color.colorInt,
                                    subSettings.backgroundStyle.bgColorInt,
                                    android.graphics.Color.TRANSPARENT,
                                    subSettings.backgroundStyle.edgeType,
                                    subSettings.backgroundStyle.edgeColorInt,
                                    typeface
                                )
                                subtitleView.setStyle(captionStyle)
                                subtitleView.setFixedTextSize(
                                    android.util.TypedValue.COMPLEX_UNIT_SP,
                                    subSettings.size.spSize
                                )
                                subtitleView.setBottomPaddingFraction(subSettings.position.bottomFraction)
                            }
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
                        onUpdateSubtitleSettings = {
                            videoPlayer.subtitlePreferencesManager.updateSettings(it)
                        },
                        onSelectPlaybackSpeed = { videoPlayer.setPlaybackSpeed(it) },
                        onClosePlayer = { finish() },
                        onDismissControls = {
                            hideControlsJob?.cancel()
                            isControlsVisible = false
                        }
                    )

                    QuickSeekFeedbackOverlay(
                        seconds = quickSeekSeconds,
                        eventId = quickSeekEventId
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
            videoPlayer.playMedia(
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

    private fun performQuickSeek(seconds: Int) {
        videoPlayer.seekRelative(seconds * 1_000L)

        quickSeekEventId += 1L
        val eventId = quickSeekEventId
        quickSeekSeconds = seconds
        quickSeekFeedbackJob?.cancel()
        quickSeekFeedbackJob = lifecycleScope.launch {
            delay(QUICK_SEEK_FEEDBACK_DURATION_MS)
            if (eventId == quickSeekEventId) {
                quickSeekSeconds = null
            }
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
                    performQuickSeek(QUICK_SEEK_SECONDS)
                    true
                } else {
                    resetControlsTimeout()
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isControlsVisible) {
                    performQuickSeek(-QUICK_SEEK_SECONDS)
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

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        quickSeekFeedbackJob?.cancel()
        videoPlayer.release()
        super.onDestroy()
    }

    private companion object {
        const val QUICK_SEEK_SECONDS = 10
        const val QUICK_SEEK_FEEDBACK_DURATION_MS = 900L
    }
}
