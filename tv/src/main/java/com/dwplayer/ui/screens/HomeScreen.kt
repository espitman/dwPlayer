@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.models.DownloadProgressInfo
import com.dwplayer.data.models.StorageInfo
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*

@Composable
fun HomeScreen(
    historyList: List<PlaybackHistoryEntity>,
    activeTasks: List<DownloadTaskEntity>,
    liveProgress: Map<String, DownloadProgressInfo>,
    smbShares: List<SmbShareEntity>,
    storageInfo: StorageInfo,
    companionUrl: String,
    onPlayMedia: (String, String, Boolean) -> Unit,
    onNavigateDownloads: () -> Unit,
    onNavigateSmb: () -> Unit,
    onNavigateArchive: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onClearHistory: () -> Unit = {}
) {
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    val featuredItem = remember(historyList) { historyList.firstOrNull() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 36.dp, top = 4.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Featured Cinematic Hero
        item {
            FeaturedHero(
                featured = featuredItem,
                storageInfo = storageInfo,
                onPlay = {
                    if (featuredItem != null) {
                        onPlayMedia(featuredItem.mediaUri, featuredItem.title, featuredItem.isSmb)
                    } else {
                        onOpenAddDialog()
                    }
                },
                onOpenAddDialog = onOpenAddDialog,
                onNavigateArchive = onNavigateArchive
            )
        }

        // 2. Active Downloads Rail (if any)
        if (activeTasks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ACTIVE DOWNLOADS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${activeTasks.size}",
                                    color = AccentPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        FocusableCard(
                            onClick = onNavigateDownloads,
                            containerColor = Color.Transparent
                        ) {
                            Text(
                                text = "View queue →",
                                color = AccentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(activeTasks, key = { it.id }) { task ->
                            val live = liveProgress[task.id]
                            HomeDownloadCard(
                                task = task,
                                live = live,
                                onClick = onNavigateDownloads
                            )
                        }
                    }
                }
            }
        }

        // 3. Your Recent Media Rail
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR RECENT MEDIA",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (historyList.isNotEmpty()) "${historyList.size} items ready to resume" else "Saved files and stream shortcuts",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    if (historyList.isNotEmpty()) {
                        FocusableCard(
                            onClick = { showClearHistoryConfirm = true },
                            containerColor = Color.Transparent,
                            focusedContainerColor = AccentRose.copy(alpha = 0.25f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, null, tint = AccentRose, modifier = Modifier.size(14.dp))
                                Text("Clear History", color = AccentRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // History Items
                    items(historyList, key = { it.mediaUri }) { item ->
                        HomeMediaCard(
                            title = item.title,
                            subtitle = if (item.isSmb) "Network Share" else "TV Storage",
                            progress = if (item.durationMs > 0) (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            timeRemaining = formatTime(item.lastPositionMs),
                            onClick = { onPlayMedia(item.mediaUri, item.title, item.isSmb) }
                        )
                    }

                    // Network Quick Card
                    item {
                        HomeActionCard(
                            title = "Network Files",
                            subtitle = if (smbShares.isNotEmpty()) "${smbShares.size} shares connected" else "Browse local servers",
                            icon = Icons.Default.CloudQueue,
                            onClick = onNavigateSmb
                        )
                    }

                    // Open Stream Card
                    item {
                        HomeActionCard(
                            title = "Open a Stream",
                            subtitle = "Paste direct media URL",
                            icon = Icons.Default.AddLink,
                            onClick = onOpenAddDialog
                        )
                    }
                }
            }
        }
    }

    if (showClearHistoryConfirm) {
        ClearHistoryConfirmDialog(
            onDismiss = { showClearHistoryConfirm = false },
            onConfirmClear = {
                showClearHistoryConfirm = false
                onClearHistory()
            }
        )
    }
}

@Composable
private fun FeaturedHero(
    featured: PlaybackHistoryEntity?,
    storageInfo: StorageInfo,
    onPlay: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onNavigateArchive: () -> Unit
) {
    val progressPercent = if (featured != null && featured.durationMs > 0) {
        ((featured.lastPositionMs.toFloat() / featured.durationMs.toFloat()) * 100).toInt().coerceIn(1, 99)
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        SurfaceDark,
                        CardDark.copy(alpha = 0.85f),
                        Color(0xFF1E2922).copy(alpha = 0.5f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            .padding(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (featured != null) "CONTINUE WATCHING" else "CINEMA HUB",
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = featured?.title ?: "dwPlayer Android TV",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (progressPercent != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$progressPercent% watched",
                                    color = AccentPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = if (featured?.isSmb == true) "Source: Local Network SMB" else "Offline TV Storage",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableCard(
                        onClick = onPlay,
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
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                            Text(
                                text = if (progressPercent != null) "Resume $progressPercent%" else "Play Media",
                                color = Color(0xFF0D0F0E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    FocusableCard(
                        onClick = onNavigateArchive,
                        containerColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Folder, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Library", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Right Info Pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "STORAGE CAPACITY",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${storageInfo.freeSpace} Free",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Total Space: ${storageInfo.totalSpace}",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HomeMediaCard(
    title: String,
    subtitle: String,
    progress: Float,
    timeRemaining: String,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(130.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = CardDark.copy(alpha = 0.7f),
        focusedContainerColor = CardDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AccentPrimary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resume", color = TextTertiary, fontSize = 10.sp)
                    Text(timeRemaining, color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(130.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = CardDark.copy(alpha = 0.4f),
        focusedContainerColor = CardDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HomeDownloadCard(
    task: DownloadTaskEntity,
    live: DownloadProgressInfo?,
    onClick: () -> Unit
) {
    val progress = live?.progress ?: task.progress
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(110.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = CardDark.copy(alpha = 0.7f),
        focusedContainerColor = CardDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$progress%",
                    color = AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(AccentPrimary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = live?.speed.takeIf { !it.isNullOrBlank() } ?: task.status,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = live?.timeRemaining.takeIf { !it.isNullOrBlank() } ?: "",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
private fun ClearHistoryConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmClear: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .background(SurfaceDark, RoundedCornerShape(22.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Clear Watch History?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This will remove all resume progress and clear the Continue Watching section.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
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
                        onClick = onConfirmClear,
                        modifier = Modifier.weight(1f).height(44.dp),
                        containerColor = AccentRose.copy(alpha = 0.85f),
                        focusedContainerColor = AccentRose
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Clear History", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
