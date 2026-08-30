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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.tv.material3.Text
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.data.models.DownloadProgressInfo
import com.dwplayer.ui.components.AddToPlaylistDialog
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import java.io.File

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTaskEntity>,
    liveProgress: Map<String, DownloadProgressInfo>,
    playlists: List<PlaylistWithItems> = emptyList(),
    onPlayTask: (DownloadTaskEntity) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onDeleteTask: (String, Boolean) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onAddToPlaylist: ((playlistId: String, title: String, uri: String) -> Unit)? = null,
    onCreatePlaylist: ((String) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf("ALL") } // ALL, ACTIVE, COMPLETED, PAUSED
    var selectedTaskForAction by remember { mutableStateOf<DownloadTaskEntity?>(null) }
    var showAddToPlaylistTask by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val filteredTasks = remember(tasks, selectedTab) {
        when (selectedTab) {
            "ACTIVE" -> tasks.filter { it.status == "ACTIVE" || it.status == "PENDING" }
            "COMPLETED" -> tasks.filter { it.status == "COMPLETED" }
            "PAUSED" -> tasks.filter { it.status == "PAUSED" || it.status == "FAILED" }
            else -> tasks
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Title & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DOWNLOAD MANAGER",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Manage active transfers, background downloads and completed files",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            FocusableCard(
                onClick = onOpenAddDialog,
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("New Download", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Tabs & Batch Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadTabButton("All (${tasks.size})", selectedTab == "ALL") { selectedTab = "ALL" }
                DownloadTabButton("Downloading (${tasks.count { it.status == "ACTIVE" || it.status == "PENDING" }})", selectedTab == "ACTIVE") { selectedTab = "ACTIVE" }
                DownloadTabButton("Finished (${tasks.count { it.status == "COMPLETED" }})", selectedTab == "COMPLETED") { selectedTab = "COMPLETED" }
                DownloadTabButton("Paused (${tasks.count { it.status == "PAUSED" }})", selectedTab == "PAUSED") { selectedTab = "PAUSED" }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(Icons.Default.PlayArrow, "Resume All", onResumeAll, AccentEmerald)
                ToolbarButton(Icons.Default.Pause, "Pause All", onPauseAll, AccentAmber)
            }
        }

        // 3. Tasks List
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DownloadDone, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No download tasks in this category", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val live = liveProgress[task.id]
                    DownloadTaskRow(
                        task = task,
                        live = live,
                        onClick = {
                            if (task.status == "COMPLETED") {
                                onPlayTask(task)
                            } else {
                                selectedTaskForAction = task
                            }
                        },
                        onActionClick = { selectedTaskForAction = task }
                    )
                }
            }
        }
    }

    // Modal Task Actions
    selectedTaskForAction?.let { task ->
        TaskActionDialog(
            task = task,
            onDismiss = { selectedTaskForAction = null },
            onPlay = {
                selectedTaskForAction = null
                onPlayTask(task)
            },
            onAddToPlaylistPrompt = {
                val target = task
                selectedTaskForAction = null
                showAddToPlaylistTask = target
            },
            onPause = {
                selectedTaskForAction = null
                onPauseTask(task.id)
            },
            onResume = {
                selectedTaskForAction = null
                onResumeTask(task.id)
            },
            onDelete = { deleteFile ->
                selectedTaskForAction = null
                onDeleteTask(task.id, deleteFile)
            }
        )
    }

    // Add To Playlist Dialog
    showAddToPlaylistTask?.let { task ->
        val file = File(task.targetFolder, task.fileName)
        AddToPlaylistDialog(
            videoTitle = task.fileName,
            videoUri = file.absolutePath,
            playlists = playlists,
            onDismiss = { showAddToPlaylistTask = null },
            onAddToPlaylist = { playlistId, title, uri ->
                onAddToPlaylist?.invoke(playlistId, title, uri)
            },
            onCreateNewPlaylist = { name ->
                onCreatePlaylist?.invoke(name)
            }
        )
    }
}

@Composable
private fun DownloadTaskRow(
    task: DownloadTaskEntity,
    live: DownloadProgressInfo?,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val progress = live?.progress ?: task.progress
    val isCompleted = task.status == "COMPLETED"
    val isActive = task.status == "ACTIVE"

    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Title + Status Info
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) AccentEmerald.copy(alpha = 0.15f)
                            else if (isActive) AccentPrimary.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else if (isActive) Icons.Default.Download else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (isCompleted) AccentEmerald else if (isActive) AccentSecondary else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.fileName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.status,
                            color = if (isCompleted) AccentEmerald else if (isActive) AccentAmber else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (task.totalBytes > 0) {
                            Text("•", color = TextTertiary, fontSize = 10.sp)
                            Text(formatBytes(task.downloadedBytes) + " / " + formatBytes(task.totalBytes), color = TextTertiary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Speed / Progress Gauge
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = live?.speed.takeIf { !it.isNullOrBlank() } ?: if (isCompleted) "Downloaded" else "",
                        color = AccentAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("$progress%", color = if (isCompleted) AccentEmerald else AccentSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isCompleted) AccentEmerald else AccentPrimary)
                    )
                }
            }

            // Action Quick Indicator
            Spacer(modifier = Modifier.width(16.dp))
            FocusableCard(
                onClick = onActionClick,
                containerColor = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.PlayArrow else Icons.Default.MoreVert,
                        contentDescription = "Action",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskActionDialog(
    task: DownloadTaskEntity,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylistPrompt: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = task.fileName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                if (task.status == "COMPLETED") {
                    DialogActionButton(Icons.Default.PlayArrow, "Play Movie Now", AccentEmerald, onPlay)
                    DialogActionButton(Icons.Default.PlaylistAdd, "Add to Playlist / Series", AccentCyan, onAddToPlaylistPrompt)
                } else if (task.status == "ACTIVE" || task.status == "PENDING") {
                    DialogActionButton(Icons.Default.Pause, "Pause Download", AccentAmber, onPause)
                } else {
                    DialogActionButton(Icons.Default.PlayArrow, "Resume Download", AccentPrimary, onResume)
                }

                DialogActionButton(Icons.Default.Delete, "Delete File & Task", AccentRose) { onDelete(true) }
                DialogActionButton(Icons.Default.DeleteOutline, "Remove Task from List Only", Color.White.copy(alpha = 0.7f)) { onDelete(false) }

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
private fun DialogActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        containerColor = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DownloadTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    FocusableCard(
        onClick = onClick,
        containerColor = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.06f),
        focusedContainerColor = AccentSecondary
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color) {
    FocusableCard(
        onClick = onClick,
        containerColor = Color.White.copy(alpha = 0.05f),
        focusedContainerColor = tint
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, sizes.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, i.toDouble()), sizes[i])
}
