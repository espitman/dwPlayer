package com.dwplayer.core.player

import androidx.media3.ui.AspectRatioFrameLayout

data class TrackInfo(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

enum class VideoResizeMode(val value: Int, val displayName: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit Screen"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Crop / Zoom"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Stretch"),
    FIXED_16_9(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, "16:9"),
    FIXED_4_3(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, "4:3")
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val title: String = "",
    val mediaUri: String = "",
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val currentAudioIndex: Int = -1,
    val currentSubtitleIndex: Int = -1,
    val subtitleDelayMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val isControlsVisible: Boolean = true,
    val isEnded: Boolean = false,
    val errorMessage: String? = null
)
