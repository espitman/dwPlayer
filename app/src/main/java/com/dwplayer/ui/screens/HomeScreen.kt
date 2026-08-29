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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
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
    onOpenAddDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Hero Banner
        item {
            HeroBanner(
                storageInfo = storageInfo,
                companionUrl = companionUrl,
                onOpenAddDialog = onOpenAddDialog,
                onNavigateSmb = onNavigateSmb
            )
        }

        // 2. Active Downloads Section (if any)
        if (activeTasks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE DOWNLOADS (${activeTasks.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        FocusableCard(
                            onClick = onNavigateDownloads,
                            containerColor = Color.Transparent,
                            focusedContainerColor = AccentPrimary.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "View All →",
                                color = AccentSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

        // 3. Continue Watching / Recent History
        if (historyList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "CONTINUE WATCHING",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(historyList, key = { it.mediaUri }) { item ->
                            HomeHistoryCard(
                                item = item,
                                onClick = { onPlayMedia(item.mediaUri, item.title, item.isSmb) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Network SMB Quick Access
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCAL NETWORK SHARES (SMB)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    FocusableCard(
                        onClick = onNavigateSmb,
                        containerColor = Color.Transparent,
                        focusedContainerColor = AccentPrimary.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "Browse All →",
                            color = AccentSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (smbShares.isEmpty()) {
                    FocusableCard(
                        onClick = onNavigateSmb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, "Add SMB", tint = AccentSecondary, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("Connect PC, Mac or NAS via SMB", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Stream movies across local home Wi-Fi directly without downloading", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(smbShares, key = { it.id }) { share ->
                            HomeSmbCard(share = share, onClick = onNavigateSmb)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    storageInfo: StorageInfo,
    companionUrl: String,
    onOpenAddDialog: () -> Unit,
    onNavigateSmb: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1E3A8A).copy(alpha = 0.8f),
                        Color(0xFF1E1B4B).copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "dwPlayer Cinema Hub",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Download movies via URL or stream directly from your local network PC/NAS.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableCard(
                        onClick = onOpenAddDialog,
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddLink, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Download Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableCard(
                        onClick = onNavigateSmb,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CloudQueue, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Network", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Storage & Web Remote Widget
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text("TV Web Remote", color = AccentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(companionUrl, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Free Space: ${storageInfo.freeSpace} / ${storageInfo.totalSpace}", color = TextSecondary, fontSize = 10.sp)
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
            .height(110.dp)
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
                    color = AccentSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
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
                    color = AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = live?.timeRemaining.takeIf { !it.isNullOrBlank() } ?: "",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HomeHistoryCard(
    item: PlaybackHistoryEntity,
    onClick: () -> Unit
) {
    val progress = if (item.durationMs > 0) (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (item.isSmb) Icons.Default.FolderShared else Icons.Default.Movie,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AccentSecondary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resume", color = TextSecondary, fontSize = 10.sp)
                    Text(formatTime(item.lastPositionMs), color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeSmbCard(
    share: SmbShareEntity,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FolderShared, null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                Text(share.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text("smb://${share.host}/${share.shareName}", color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
