@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.player

import androidx.compose.animation.*
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.dwplayer.core.player.PlayerUiState
import com.dwplayer.core.player.TrackInfo
import com.dwplayer.core.player.VideoResizeMode
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
    onSelectResizeMode: (VideoResizeMode) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onClosePlayer: () -> Unit,
    onDismissControls: () -> Unit = {}
) {
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val playPauseFocusRequester = remember { FocusRequester() }

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
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(32.dp)
        ) {
            // TOP BAR: Title + Back + Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
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
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = AccentRose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.resizeMode.displayName} • Speed ${uiState.playbackSpeed}x",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Settings (Subtitles, Audio, Aspect, Speed)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerTopButton(Icons.Default.Subtitles, "Subs") { showSubtitleDialog = true }
                    PlayerTopButton(Icons.Default.Audiotrack, "Audio") { showAudioDialog = true }
                    PlayerTopButton(Icons.Default.AspectRatio, "Aspect") { showAspectDialog = true }
                    PlayerTopButton(Icons.Default.Speed, "${uiState.playbackSpeed}x") { showSpeedDialog = true }
                }
            }

            // CENTER: Large Play / Pause & Seek & Episode Skip buttons
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPreviousEpisode) {
                    FocusableCard(
                        onClick = onPlayPreviousEpisode,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = AccentPrimary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SkipPrevious, "Previous Episode", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                FocusableCard(
                    onClick = onSeekBackward,
                    containerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = AccentPrimary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                FocusableCard(
                    onClick = onTogglePlayPause,
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary,
                    modifier = Modifier
                        .size(72.dp)
                        .focusRequester(playPauseFocusRequester)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                FocusableCard(
                    onClick = onSeekForward,
                    containerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = AccentPrimary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                if (hasNextEpisode) {
                    FocusableCard(
                        onClick = onPlayNextEpisode,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = AccentPrimary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SkipNext, "Next Episode", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // BOTTOM BAR: Focusable Progress Bar & Timestamps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val progress = if (uiState.durationMs > 0) (uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                val bufferedProgress = if (uiState.durationMs > 0) (uiState.bufferedPositionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                TvSeekBar(
                    progress = progress,
                    bufferedProgress = bufferedProgress,
                    onSeekLeft = onSeekBackward,
                    onSeekRight = onSeekForward,
                    onTogglePlay = onTogglePlayPause,
                    onDismiss = onDismissControls
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatDuration(uiState.currentPositionMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        formatDuration(uiState.durationMs),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
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
        TrackSelectionDialog(
            title = "Subtitles",
            tracks = uiState.subtitleTracks,
            selectedIndex = uiState.currentSubtitleIndex,
            allowDisable = true,
            onSelect = {
                onSelectSubtitleTrack(it)
                showSubtitleDialog = false
            },
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

    if (showAspectDialog) {
        OptionDialog(
            title = "Aspect Ratio",
            options = VideoResizeMode.values().toList(),
            selectedOption = uiState.resizeMode,
            getLabel = { it.displayName },
            onSelect = {
                onSelectResizeMode(it)
                showAspectDialog = false
            },
            onDismiss = { showAspectDialog = false }
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
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) CardDark.copy(alpha = 0.6f) else Color.Transparent,
            focusedContainerColor = CardDark.copy(alpha = 0.8f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = androidx.tv.material3.Border(BorderStroke(2.dp, AccentSecondary))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (isFocused) 10.dp else 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                // Buffered progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
                // Active progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isFocused) AccentSecondary else AccentPrimary)
                )
            }
        }
    }
}

@Composable
private fun PlayerTopButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FocusableCard(
        onClick = onClick,
        containerColor = Color.White.copy(alpha = 0.12f),
        focusedContainerColor = AccentPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
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
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text("Off / None", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                tracks.forEachIndexed { index, track ->
                    val isSelected = index == selectedIndex
                    FocusableCard(
                        onClick = { onSelect(track) },
                        containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text(track.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text(getLabel(opt), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
