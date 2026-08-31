@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.R
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.data.models.StorageInfo
import com.dwplayer.ui.components.AddToPlaylistDialog
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaArchiveScreen(
    files: List<LocalArchiveFile>,
    storageInfo: StorageInfo,
    playbackHistory: List<PlaybackHistoryEntity> = emptyList(),
    playlists: List<PlaylistWithItems> = emptyList(),
    onPlayFile: (LocalArchiveFile) -> Unit,
    onDeleteFile: (LocalArchiveFile) -> Unit,
    onRefresh: () -> Unit,
    onAddToPlaylist: ((playlistId: String, title: String, uri: String) -> Unit)? = null,
    onCreatePlaylist: ((String) -> Unit)? = null
) {
    var selectedFile by remember { mutableStateOf<LocalArchiveFile?>(null) }
    var actionFile by remember { mutableStateOf<LocalArchiveFile?>(null) }
    var fileToDelete by remember { mutableStateOf<LocalArchiveFile?>(null) }
    var showAddToPlaylistFile by remember { mutableStateOf<LocalArchiveFile?>(null) }

    val activeFile = remember(files, selectedFile) {
        if (selectedFile != null && files.any { it.path == selectedFile?.path }) {
            files.first { it.path == selectedFile?.path }
        } else if (files.isNotEmpty()) {
            files.first()
        } else null
    }

    LaunchedEffect(files) {
        if (selectedFile == null && files.isNotEmpty()) {
            selectedFile = files.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x142A493B), Color.Transparent),
                    radius = 680f
                )
            )
            .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "SAVED ON THIS TV",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Library",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "Offline files, recent imports and everything ready to play.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "${files.size} file${if (files.size == 1) "" else "s"} · ${storageInfo.freeSpace} free",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                FocusableCard(
                    onClick = onRefresh,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = Color.White.copy(alpha = 0.055f),
                    focusedContainerColor = CardDark,
                    borderColor = Color.White.copy(alpha = 0.12f),
                    scale = 1.035f
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Refresh", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (files.isEmpty()) {
            LibraryEmptyState(modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceDark.copy(alpha = 0.54f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                        .padding(14.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(files, key = { it.path }) { file ->
                            ArchiveFileRow(
                                file = file,
                                history = playbackHistory.forLocalFile(file),
                                artworkRes = libraryArtwork(file.name),
                                isSelected = activeFile?.path == file.path,
                                onClick = { selectedFile = file },
                                onLongClick = { actionFile = file }
                            )
                        }
                    }
                }

                activeFile?.let { file ->
                    LibraryInfoPanel(
                        file = file,
                        history = playbackHistory.forLocalFile(file),
                        artworkRes = libraryArtwork(file.name),
                        onPlay = { onPlayFile(file) },
                        modifier = Modifier.weight(0.56f).fillMaxHeight()
                    )
                }
            }
        }
    }

    actionFile?.let { file ->
        LibraryFileActionDialog(
            file = file,
            onPlay = {
                actionFile = null
                onPlayFile(file)
            },
            onAddToSeries = {
                actionFile = null
                showAddToPlaylistFile = file
            },
            onDelete = {
                actionFile = null
                fileToDelete = file
            },
            onDismiss = { actionFile = null }
        )
    }

    // Add to Playlist Dialog
    showAddToPlaylistFile?.let { file ->
        AddToPlaylistDialog(
            videoTitle = file.name,
            videoUri = file.path,
            playlists = playlists,
            onDismiss = { showAddToPlaylistFile = null },
            onAddToPlaylist = { playlistId, title, uri ->
                onAddToPlaylist?.invoke(playlistId, title, uri)
            },
            onCreateNewPlaylist = { name ->
                onCreatePlaylist?.invoke(name)
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { target ->
        DeleteArchiveConfirmDialog(
            file = target,
            onDismiss = { fileToDelete = null },
            onConfirmDelete = {
                onDeleteFile(target)
                fileToDelete = null
            }
        )
    }
}

@Composable
private fun LibraryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark.copy(alpha = 0.54f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.White.copy(alpha = 0.055f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, null, tint = TextSecondary, modifier = Modifier.size(31.dp))
            }
            Text("No saved videos yet", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(
                "Download movies via URL or transfer files across your local network.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LibraryInfoPanel(
    file: LocalArchiveFile,
    history: PlaybackHistoryEntity?,
    artworkRes: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = history.progressFraction()
    val durationMs = file.durationMs.takeIf { it > 0L } ?: history?.durationMs ?: 0L
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark.copy(alpha = 0.62f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(artworkRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = libraryDisplayTitle(file.name),
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Saved ${file.lastModified.asLibraryDate()} and available without a network connection.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))
            SpecRow(
                "Quality",
                file.videoQualityLabel()
            )
            SpecRow("Storage", file.sizeFormatted)
            SpecRow("Duration", durationMs.asLibraryDuration())

            Spacer(Modifier.weight(1f))

            FocusableCard(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(14.dp),
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary,
                contentColor = BgDark,
                focusedContentColor = BgDark,
                borderColor = Color.Transparent,
                focusedBorderColor = Color.White,
                scale = 1.035f
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = BgDark, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (progress > 0f) "Resume ${(progress * 100).toInt()}%" else "Play", color = BgDark, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun libraryArtwork(fileName: String): Int {
    val fallbackImages = intArrayOf(
        R.drawable.fallback_coast,
        R.drawable.fallback_forest,
        R.drawable.fallback_platform,
        R.drawable.fallback_motel,
        R.drawable.fallback_rain_room,
        R.drawable.fallback_lake
    )
    val safeHash = if (fileName.hashCode() == Int.MIN_VALUE) 0 else kotlin.math.abs(fileName.hashCode())
    return fallbackImages[safeHash % fallbackImages.size]
}

private fun libraryDisplayTitle(fileName: String): String {
    val raw = fileName.substringBeforeLast('.', fileName).replace('.', ' ').replace('_', ' ')
    return raw.split(' ')
        .filter {
            it.isNotBlank() &&
                it.none(Char::isDigit) &&
                it.lowercase() !in setOf("dvdrip", "brrip", "webrip", "bluray")
        }
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.titlecase() } }
        .ifBlank { fileName.substringBeforeLast('.', fileName) }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(0.dp))
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ArchiveFileRow(
    file: LocalArchiveFile,
    history: PlaybackHistoryEntity?,
    artworkRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val durationMs = file.durationMs.takeIf { it > 0L } ?: history?.durationMs ?: 0L
    val progress = history.progressFraction()
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isSelected) CardElevated.copy(alpha = 0.88f) else SurfaceDark.copy(alpha = 0.82f),
        focusedContainerColor = CardDark,
        borderColor = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        focusedBorderColor = AccentPrimary,
        scale = 1.015f,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(artworkRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(96.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.extension.uppercase(Locale.getDefault()),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("•", color = TextTertiary, fontSize = 10.sp)
                        Text(
                            text = file.sizeFormatted,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("•", color = TextTertiary, fontSize = 10.sp)
                        Text(
                            text = durationMs.asLibraryDuration(),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    when {
                        history?.isCompleted == true -> "Watched"
                        progress > 0f -> "${(progress * 100).toInt()}% watched"
                        else -> "Ready"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun List<PlaybackHistoryEntity>.forLocalFile(file: LocalArchiveFile): PlaybackHistoryEntity? {
    val targetPath = file.path.removePrefix("file://")
    return firstOrNull { it.mediaUri.removePrefix("file://") == targetPath }
}

private fun PlaybackHistoryEntity?.progressFraction(): Float {
    if (this == null || durationMs <= 0L || isCompleted) return 0f
    return (lastPositionMs.toFloat() / durationMs).coerceIn(0f, 0.99f)
}

private fun LocalArchiveFile.videoQualityLabel(): String = when {
    videoHeight >= 2160 -> "4K · ${videoWidth}×${videoHeight}"
    videoHeight >= 1440 -> "QHD · ${videoWidth}×${videoHeight}"
    videoHeight >= 1080 -> "FULL HD · ${videoWidth}×${videoHeight}"
    videoHeight >= 720 -> "HD · ${videoWidth}×${videoHeight}"
    videoHeight > 0 -> "SD · ${videoWidth}×${videoHeight}"
    else -> "$extension · LOCAL"
}

private fun Long.asLibraryDuration(): String {
    if (this <= 0L) return "Unknown"
    val totalSeconds = this / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun Long.asLibraryDate(): String {
    if (this <= 0L) return "on this TV"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this))
}

@Composable
private fun LibraryFileActionDialog(
    file: LocalArchiveFile,
    onPlay: () -> Unit,
    onAddToSeries: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    file.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Choose an action for this saved file.", color = TextSecondary, fontSize = 12.sp)

                FocusableCard(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary,
                    contentColor = BgDark,
                    focusedContentColor = BgDark
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Play", color = BgDark, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }

                FocusableCard(
                    onClick = onAddToSeries,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    containerColor = CardDark
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Add to Series", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FocusableCard(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    containerColor = AccentRose.copy(alpha = 0.14f),
                    focusedContainerColor = AccentRose
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Delete file", color = AccentRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteArchiveConfirmDialog(
    file: LocalArchiveFile,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Delete Video File?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Permanently delete \"${file.name}\" (${file.sizeFormatted}) from TV storage?",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableCard(
                        onClick = onConfirmDelete,
                        modifier = Modifier.weight(1f).height(44.dp),
                        containerColor = AccentRose.copy(alpha = 0.85f),
                        focusedContainerColor = AccentRose
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Delete File", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
