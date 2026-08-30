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
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.data.entities.PlaylistWithItems
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
    playlists: List<PlaylistWithItems> = emptyList(),
    onPlayFile: (LocalArchiveFile) -> Unit,
    onDeleteFile: (LocalArchiveFile) -> Unit,
    onRefresh: () -> Unit,
    onAddToPlaylist: ((playlistId: String, title: String, uri: String) -> Unit)? = null,
    onCreatePlaylist: ((String) -> Unit)? = null
) {
    var selectedFile by remember { mutableStateOf<LocalArchiveFile?>(null) }
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
            .padding(start = 24.dp, end = 36.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Offline files, recent imports and everything ready to play.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            // Storage pill & Refresh
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${storageInfo.freeSpace} Free",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                FocusableCard(
                    onClick = onRefresh,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Refresh", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Main Content: Master-Detail Split
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                    }
                    Text(
                        text = "No saved videos yet",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Download movies via URL or transfer files across your local network.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: List of Files
                LazyColumn(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(files, key = { it.path }) { file ->
                        val isSelected = activeFile?.path == file.path
                        ArchiveFileRow(
                            file = file,
                            isSelected = isSelected,
                            onClick = { selectedFile = file },
                            onDoubleClick = { onPlayFile(file) }
                        )
                    }
                }

                // Right Column: Detail / Info Panel
                if (activeFile != null) {
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceDark.copy(alpha = 0.8f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Large Preview Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(CardDark, Color(0xFF1E2822))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = AccentPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = activeFile.extension.uppercase(Locale.getDefault()),
                                            color = AccentPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = activeFile.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Specs List
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpecRow("Size", activeFile.sizeFormatted)
                                    SpecRow("Format", activeFile.extension.uppercase(Locale.getDefault()))
                                    SpecRow("Location", "TV Storage")
                                }
                            }

                            // Actions
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FocusableCard(
                                    onClick = { onPlayFile(activeFile) },
                                    containerColor = AccentPrimary,
                                    focusedContainerColor = AccentSecondary,
                                    contentColor = Color(0xFF0D0F0E),
                                    focusedContentColor = Color(0xFF0D0F0E),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Movie Now", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FocusableCard(
                                        onClick = { showAddToPlaylistFile = activeFile },
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("+ Series", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    FocusableCard(
                                        onClick = { fileToDelete = activeFile },
                                        containerColor = AccentRose.copy(alpha = 0.15f),
                                        focusedContainerColor = AccentRose,
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Delete", color = AccentRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 4.dp),
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
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onDoubleClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else CardDark.copy(alpha = 0.7f),
        focusedContainerColor = CardDark,
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.sizeFormatted,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("•", color = TextTertiary, fontSize = 10.sp)
                        Text(
                            text = file.extension.uppercase(Locale.getDefault()),
                            color = AccentPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
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
                Text("Ready", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
