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
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.data.models.DownloadProgressInfo
import com.dwplayer.ui.components.AddToPlaylistDialog
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import java.io.File
import java.util.Locale

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
    var selectedTab by remember { mutableStateOf("ALL") }
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
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x142A493B), Color.Transparent),
                    radius = 680f
                )
            )
            .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header (Matching screenshot-2026-08-30T16-38-03-301Z.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "OFFLINE QUEUE",
                    color = Color(0xFF8A968F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Downloads",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2.5).sp,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Keep streams available for later. Active tasks will appear here with clear progress and speed.",
                    color = Color(0xFFA2ADA6),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.widthIn(max = 580.dp)
                )
            }

            FocusableCard(
                onClick = onOpenAddDialog,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFFB8F53A),
                focusedContainerColor = Color(0xFFCEF22C),
                contentColor = Color(0xFF000000),
                focusedContentColor = Color(0xFF000000)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF000000), modifier = Modifier.size(20.dp))
                    Text("New download", color = Color(0xFF000000), fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 2. Main Content Area
        if (filteredTasks.isEmpty()) {
            // Large empty state card exactly as in screenshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark.copy(alpha = 0.48f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF7A857E),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "No downloads yet",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )

                    Text(
                        text = "Add a direct media URL. Download progress, speed and remaining time will stay visible here.",
                        color = Color(0xFF8A968F),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 480.dp)
                    )
                }
            }
        } else {
            // Task List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark.copy(alpha = 0.48f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        onLongClick = onActionClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xFF181D1A),
        focusedContainerColor = Color(0xFF222824),
        focusedBorderColor = Color(0xFFB8F53A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) Color(0xFF10B981).copy(alpha = 0.15f)
                            else if (isActive) Color(0xFFB8F53A).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else if (isActive) Icons.Default.Download else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF10B981) else if (isActive) Color(0xFFB8F53A) else Color(0xFFA2ADA6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.fileName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.status,
                            color = if (isCompleted) Color(0xFF10B981) else if (isActive) Color(0xFFB8F53A) else Color(0xFFA2ADA6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (task.totalBytes > 0) {
                            Text("•", color = Color(0xFF6B726E), fontSize = 10.sp)
                            Text(
                                text = "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}",
                                color = Color(0xFF8A968F),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = live?.speed.takeIf { !it.isNullOrBlank() } ?: if (isCompleted) "Ready" else "",
                        color = Color(0xFFA2ADA6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$progress%",
                        color = if (isCompleted) Color(0xFF10B981) else Color(0xFFB8F53A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isCompleted) Color(0xFF10B981) else Color(0xFFB8F53A))
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            FocusableCard(
                onClick = onActionClick,
                containerColor = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.size(38.dp),
                shape = CircleShape
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
                .width(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF121614))
                .border(1.dp, Color(0xFF222824), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = task.fileName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                if (task.status == "COMPLETED") {
                    DialogActionButton(Icons.Default.PlayArrow, "Play Movie Now", Color(0xFF10B981), onPlay)
                    DialogActionButton(Icons.Default.PlaylistAdd, "Add to Playlist / Series", Color(0xFFB8F53A), onAddToPlaylistPrompt)
                } else if (task.status == "ACTIVE" || task.status == "PENDING") {
                    DialogActionButton(Icons.Default.Pause, "Pause Download", Color(0xFFFFB703), onPause)
                } else {
                    DialogActionButton(Icons.Default.PlayArrow, "Resume Download", Color(0xFFB8F53A), onResume)
                }

                DialogActionButton(Icons.Default.Delete, "Delete File & Task", Color(0xFFEF4444)) { onDelete(true) }
                DialogActionButton(Icons.Default.DeleteOutline, "Remove Task from List Only", Color.White.copy(alpha = 0.7f)) { onDelete(false) }

                FocusableCard(
                    onClick = onDismiss,
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = Color(0xFF8A968F), fontSize = 12.sp)
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
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        Locale.US,
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}
