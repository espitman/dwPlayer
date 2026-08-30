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
                    text = "OFFLINE QUEUE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Downloads",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Keep streams available for later. Active tasks will appear here with clear progress and speed.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            FocusableCard(
                onClick = onOpenAddDialog,
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary,
                contentColor = Color(0xFF0D0F0E),
                focusedContentColor = Color(0xFF0D0F0E)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                    Text("New download", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 2. Tabs Bar (if tasks exist)
        if (tasks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "ACTIVE", "COMPLETED", "PAUSED").forEach { tab ->
                        DownloadTabButton(
                            text = tab,
                            isSelected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusableCard(
                        onClick = onPauseAll,
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = "Pause All",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    FocusableCard(
                        onClick = onResumeAll,
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = "Resume All",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. Content Panel
        if (filteredTasks.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "No downloads yet",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Add a direct media URL. Download progress, speed and remaining time will stay visible here.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 480.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
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
        onLongClick = onActionClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = CardDark.copy(alpha = 0.7f),
        focusedContainerColor = CardDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Title + Status
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) AccentEmerald.copy(alpha = 0.15f)
                            else if (isActive) AccentPrimary.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else if (isActive) Icons.Default.Download else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (isCompleted) AccentEmerald else if (isActive) AccentPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = task.fileName,
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
                            text = task.status,
                            color = if (isCompleted) AccentEmerald else if (isActive) AccentPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (task.totalBytes > 0) {
                            Text("•", color = TextTertiary, fontSize = 10.sp)
                            Text(
                                text = "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Progress & Speed
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = live?.speed.takeIf { !it.isNullOrBlank() } ?: if (isCompleted) "Ready" else "",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$progress%",
                        color = if (isCompleted) AccentEmerald else AccentPrimary,
                        fontSize = 12.sp,
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
                            .background(if (isCompleted) AccentEmerald else AccentPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            FocusableCard(
                onClick = onActionClick,
                containerColor = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.size(36.dp),
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
private fun DownloadTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    FocusableCard(
        onClick = onClick,
        containerColor = if (isSelected) Color.White else Color.Transparent,
        focusedContainerColor = if (isSelected) Color.White else CardDark,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.Black else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
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
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
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
                    DialogActionButton(Icons.Default.PlaylistAdd, "Add to Playlist / Series", AccentPrimary, onAddToPlaylistPrompt)
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
