@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.data.models.StorageInfo
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaArchiveScreen(
    files: List<LocalArchiveFile>,
    storageInfo: StorageInfo,
    onPlayFile: (LocalArchiveFile) -> Unit,
    onDeleteFile: (LocalArchiveFile) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedActionFile by remember { mutableStateOf<LocalArchiveFile?>(null) }
    var fileToDelete by remember { mutableStateOf<LocalArchiveFile?>(null) }

    val totalBytes = remember(files) {
        files.sumOf { it.sizeBytes }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIDEO ARCHIVE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saved video files on TV storage • ${files.size} videos (${formatBytesTotal(totalBytes)})",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Storage pill & Refresh
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(CardDark.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${storageInfo.freeSpace} Free",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Text(
                            text = "/ ${storageInfo.totalSpace}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                FocusableCard(
                    onClick = onRefresh,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = AccentPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Refresh",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }
        }

        // 2. Videos List
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No video files found in local storage",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloaded movie and video files saved to TV will appear here",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    ArchiveFileCard(
                        file = file,
                        onClick = { onPlayFile(file) },
                        onLongClick = { selectedActionFile = file }
                    )
                }
            }
        }
    }

    // 3. File Actions Dialog (Play Movie / Delete / Cancel)
    selectedActionFile?.let { file ->
        ArchiveFileActionDialog(
            file = file,
            onDismiss = { selectedActionFile = null },
            onPlay = {
                selectedActionFile = null
                onPlayFile(file)
            },
            onDeletePrompt = {
                selectedActionFile = null
                fileToDelete = file
            }
        )
    }

    // 4. Delete Confirmation Dialog
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
private fun ArchiveFileCard(
    file: LocalArchiveFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = remember(file.lastModified) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))
    }

    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = CardDark.copy(alpha = 0.55f),
        focusedContainerColor = CardDark.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Movie Icon + File Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(AccentPrimary.copy(alpha = 0.8f), Color(0xFF4F46E5)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ext Badge
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = file.extension,
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = file.sizeFormatted,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text("•", color = TextTertiary, fontSize = 11.sp)

                        Text(
                            text = dateStr,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Right: Action Hint Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hold for Options", color = TextTertiary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .background(AccentPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = AccentSecondary, modifier = Modifier.size(14.dp))
                        Text("Play", color = AccentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveFileActionDialog(
    file: LocalArchiveFile,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onDeletePrompt: () -> Unit
) {
    val headerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            headerFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Focusable Safe Info Header (Lands initial focus safely)
                FocusableCard(
                    onClick = {},
                    containerColor = CardDark.copy(alpha = 0.6f),
                    focusedContainerColor = CardDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(headerFocusRequester)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${file.sizeFormatted} • Format: ${file.extension}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                DialogActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Play Movie Now",
                    color = AccentEmerald,
                    onClick = onPlay
                )

                DialogActionButton(
                    icon = Icons.Default.DeleteForever,
                    label = "Delete from TV Storage",
                    color = AccentRose,
                    onClick = onDeletePrompt
                )

                FocusableCard(
                    onClick = onDismiss,
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = TextTertiary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val openedAt = remember { System.currentTimeMillis() }
    FocusableCard(
        onClick = {
            if (System.currentTimeMillis() - openedAt > 500L) {
                onClick()
            }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        containerColor = Color.White.copy(alpha = 0.05f),
        focusedContainerColor = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}

@Composable
private fun DeleteArchiveConfirmDialog(
    file: LocalArchiveFile,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentRose.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = AccentRose, modifier = Modifier.size(24.dp))
                    }

                    Column {
                        Text("Delete File from Storage?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("This action permanently frees disk space", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(file.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Size: ${file.sizeFormatted} • Location: TV Storage", color = TextTertiary, fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Are you sure you want to permanently delete this file? You will not be able to recover it.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusableCard(
                        onClick = onConfirmDelete,
                        containerColor = AccentRose,
                        focusedContainerColor = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Delete Permanently",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        containerColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytesTotal(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
}
