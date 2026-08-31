@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.dwplayer.core.player.*
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*

@Composable
fun PlayerOverlayControls(
    uiState: PlayerUiState,
    isVisible: Boolean,
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    onPlayPreviousEpisode: () -> Unit = {},
    onPlayNextEpisode: () -> Unit = {},
    showAutoNextOverlay: Boolean = false,
    autoNextTitle: String = "",
    autoNextSeconds: Long = 0L,
    onDismissAutoNext: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSelectAudioTrack: (TrackInfo) -> Unit,
    onSelectSubtitleTrack: (TrackInfo?) -> Unit,
    onUpdateSubtitleSettings: (SubtitleSettings) -> Unit = {},
    onSelectPlaybackSpeed: (Float) -> Unit,
    onClosePlayer: () -> Unit,
    onDismissControls: () -> Unit = {}
) {
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val speedFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            try {
                playPauseFocusRequester.requestFocus()
            } catch (ignored: Exception) {}
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BgDark.copy(alpha = 0.34f),
                            BgDark.copy(alpha = 0.18f),
                            BgDark.copy(alpha = 0.78f)
                        )
                    )
                )
        ) {
            // TOP BAR: Title + Back + Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 48.dp, top = 38.dp, end = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FocusableCard(
                        onClick = onClosePlayer,
                        containerColor = BgDark.copy(alpha = 0.48f),
                        focusedContainerColor = BgDark.copy(alpha = 0.72f),
                        borderColor = Color.White.copy(alpha = 0.20f),
                        focusedBorderColor = AccentPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = playerFileName(uiState),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick settings match the web player's compact text tools.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerTopButton("Subtitles") { showSubtitleDialog = true }
                    PlayerTopButton("Audio") { showAudioDialog = true }
                    PlayerTopButton(
                        label = formatPlaybackSpeed(uiState.playbackSpeed),
                        modifier = Modifier
                            .focusRequester(speedFocusRequester)
                            .focusProperties { down = playPauseFocusRequester }
                    ) { showSpeedDialog = true }
                }
            }

            // CENTER: Large Play / Pause & Seek & Episode Skip buttons
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPreviousEpisode) {
                    FocusableCard(
                        onClick = onPlayPreviousEpisode,
                        containerColor = BgDark.copy(alpha = 0.48f),
                        focusedContainerColor = BgDark.copy(alpha = 0.72f),
                        borderColor = Color.White.copy(alpha = 0.20f),
                        focusedBorderColor = AccentPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(66.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SkipPrevious, "Previous Episode", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }

                FocusableCard(
                    onClick = onSeekBackward,
                    containerColor = BgDark.copy(alpha = 0.48f),
                    focusedContainerColor = BgDark.copy(alpha = 0.72f),
                    borderColor = Color.White.copy(alpha = 0.20f),
                    focusedBorderColor = AccentPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(66.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                FocusableCard(
                    onClick = onTogglePlayPause,
                    containerColor = TextPrimary,
                    focusedContainerColor = Color.White,
                    contentColor = BgDark,
                    focusedContentColor = BgDark,
                    borderColor = Color.Transparent,
                    focusedBorderColor = AccentPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .size(88.dp)
                        .focusRequester(playPauseFocusRequester)
                        .focusProperties {
                            up = speedFocusRequester
                            down = timelineFocusRequester
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = BgDark,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                FocusableCard(
                    onClick = onSeekForward,
                    containerColor = BgDark.copy(alpha = 0.48f),
                    focusedContainerColor = BgDark.copy(alpha = 0.72f),
                    borderColor = Color.White.copy(alpha = 0.20f),
                    focusedBorderColor = AccentPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(66.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                if (hasNextEpisode) {
                    FocusableCard(
                        onClick = onPlayNextEpisode,
                        containerColor = BgDark.copy(alpha = 0.48f),
                        focusedContainerColor = BgDark.copy(alpha = 0.72f),
                        borderColor = Color.White.copy(alpha = 0.20f),
                        focusedBorderColor = AccentPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(66.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SkipNext, "Next Episode", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }

            // BOTTOM BAR: Focusable Progress Bar & Timestamps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 48.dp, end = 48.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val progress = if (uiState.durationMs > 0) (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                val bufferedProgress = if (uiState.durationMs > 0) (uiState.bufferedPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                TvSeekBar(
                    progress = progress,
                    bufferedProgress = bufferedProgress,
                    onSeekLeft = onSeekBackward,
                    onSeekRight = onSeekForward,
                    onTogglePlay = onTogglePlayPause,
                    onDismiss = onDismissControls,
                    modifier = Modifier
                        .focusRequester(timelineFocusRequester)
                        .focusProperties { up = playPauseFocusRequester }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatDuration(uiState.currentPositionMs),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        formatDuration(uiState.durationMs),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }

    // 30-Second Auto-Next Countdown Overlay Banner
    AnimatedVisibility(
        visible = showAutoNextOverlay,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AutoNextOverlayBanner(
                title = autoNextTitle,
                secondsRemaining = autoNextSeconds,
                onPlayNow = onPlayNextEpisode,
                onDismiss = onDismissAutoNext
            )
        }
    }

    // Modal Dialogs
    if (showSubtitleDialog) {
        SubtitleSettingsDialog(
            subtitleTracks = uiState.subtitleTracks,
            selectedTrackIndex = uiState.currentSubtitleIndex,
            settings = uiState.subtitleSettings,
            onSelectTrack = { onSelectSubtitleTrack(it) },
            onUpdateSettings = { onUpdateSubtitleSettings(it) },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAudioDialog) {
        TrackSelectionDialog(
            title = "Audio Tracks",
            tracks = uiState.audioTracks,
            selectedIndex = uiState.currentAudioIndex,
            allowDisable = false,
            onSelect = {
                it?.let { onSelectAudioTrack(it) }
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSpeedDialog) {
        OptionDialog(
            title = "Playback Speed",
            options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
            selectedOption = uiState.playbackSpeed,
            getLabel = { "${it}x" },
            onSelect = {
                onSelectPlaybackSpeed(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
fun QuickSeekFeedbackOverlay(
    seconds: Int?,
    eventId: Long,
    modifier: Modifier = Modifier
) {
    val isForward = (seconds ?: 0) > 0

    key(eventId) {
        AnimatedVisibility(
            visible = seconds != null,
            modifier = modifier.fillMaxSize(),
            enter = fadeIn() + scaleIn(initialScale = 0.82f),
            exit = fadeOut() + scaleOut(targetScale = 0.90f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 112.dp),
                contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(BgDark.copy(alpha = 0.88f))
                        .border(
                            width = 2.dp,
                            color = AccentPrimary.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                        contentDescription = if (isForward) "Forward 10 seconds" else "Rewind 10 seconds",
                        tint = AccentPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = if (isForward) "+10s" else "−10s",
                        color = Color.White,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isForward) "FORWARD" else "REWIND",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSeekBar(
    progress: Float,
    bufferedProgress: Float,
    onSeekLeft: () -> Unit,
    onSeekRight: () -> Unit,
    onTogglePlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val trackHeight by animateDpAsState(
        targetValue = if (isFocused) 7.dp else 5.dp,
        label = "trackHeight"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 0.dp,
        label = "thumbSize"
    )

    Surface(
        onClick = onTogglePlay,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onSeekLeft()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onSeekRight()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDismiss()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            onTogglePlay()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = androidx.tv.material3.Border(BorderStroke(0.dp, Color.Transparent))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(17.dp)
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                // Buffered progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedProgress)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.30f))
                )
                // Active progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }

            // Scrubber Thumb
            if (isFocused && thumbSize > 0.dp) {
                val thumbOffset = ((maxWidth - thumbSize) * progress).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(thumbSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(AccentPrimary.copy(alpha = 0.35f), CircleShape)
                    )
                    // Inner white core
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, AccentPrimary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTopButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        containerColor = BgDark.copy(alpha = 0.48f),
        focusedContainerColor = BgDark.copy(alpha = 0.72f),
        contentColor = Color.White,
        focusedContentColor = Color.White,
        borderColor = Color.White.copy(alpha = 0.20f),
        focusedBorderColor = AccentPrimary,
        shape = RoundedCornerShape(13.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

private fun playerFileName(uiState: PlayerUiState): String {
    val raw = uiState.mediaUri
        .substringBefore('?')
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?: uiState.title
    return runCatching { android.net.Uri.decode(raw) }.getOrDefault(raw)
}

private fun formatPlaybackSpeed(speed: Float): String {
    val value = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "$value×"
}

@Composable
private fun TrackSelectionDialog(
    title: String,
    tracks: List<TrackInfo>,
    selectedIndex: Int,
    allowDisable: Boolean,
    onSelect: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

                if (allowDisable) {
                    FocusableCard(
                        onClick = { onSelect(null) },
                        containerColor = if (selectedIndex == -1) AccentPrimary else Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = if (selectedIndex == -1) AccentSecondary else CardDark,
                        contentColor = if (selectedIndex == -1) BgDark else TextPrimary,
                        focusedContentColor = if (selectedIndex == -1) BgDark else TextPrimary,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Off / None", color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                tracks.forEachIndexed { index, track ->
                    val isSelected = index == selectedIndex
                    FocusableCard(
                        onClick = { onSelect(track) },
                        containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                        contentColor = if (isSelected) BgDark else TextPrimary,
                        focusedContentColor = if (isSelected) BgDark else TextPrimary,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text(track.label, color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    getLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

                options.forEach { opt ->
                    val isSelected = opt == selectedOption
                    FocusableCard(
                        onClick = { onSelect(opt) },
                        containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = if (isSelected) AccentSecondary else CardDark,
                        contentColor = if (isSelected) BgDark else TextPrimary,
                        focusedContentColor = if (isSelected) BgDark else TextPrimary,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text(getLabel(opt), color = LocalContentColor.current, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun AutoNextOverlayBanner(
    title: String,
    secondsRemaining: Long,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark.copy(alpha = 0.95f))
            .border(1.5.dp, AccentPrimary.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .width(360.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SkipNext, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("NEXT EPISODE", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Text(
                text = "Playing automatically in ${secondsRemaining}s...",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusableCard(
                    onClick = onPlayNow,
                    containerColor = AccentEmerald,
                    focusedContainerColor = Color(0xFF059669),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Play Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                FocusableCard(
                    onClick = onDismiss,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.weight(0.7f).height(38.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Dismiss", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
