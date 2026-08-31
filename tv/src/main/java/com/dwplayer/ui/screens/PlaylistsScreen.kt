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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.dwplayer.data.entities.PlaylistItemEntity
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.ui.components.*
import com.dwplayer.ui.theme.*

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithItems>,
    archiveFiles: List<LocalArchiveFile> = emptyList(),
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveItemFromPlaylist: (String) -> Unit,
    onAddItemsToPlaylist: (String, List<LocalArchiveFile>) -> Unit = { _, _ -> },
    onPlayItem: (PlaylistItemEntity, List<PlaylistItemEntity>) -> Unit,
    onPlayAll: (PlaylistWithItems) -> Unit
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistWithItems?>(null) }
    var itemToRemove by remember { mutableStateOf<PlaylistItemEntity?>(null) }
    var showStoragePickerPlaylistId by remember { mutableStateOf<String?>(null) }

    val activePlaylist = remember(playlists, selectedPlaylistId) {
        if (selectedPlaylistId != null && playlists.any { it.playlist.id == selectedPlaylistId }) {
            playlists.first { it.playlist.id == selectedPlaylistId }
        } else if (playlists.isNotEmpty()) {
            playlists.first()
        } else null
    }

    LaunchedEffect(playlists) {
        if (selectedPlaylistId == null && playlists.isNotEmpty()) {
            selectedPlaylistId = playlists.first().playlist.id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 42.dp, end = 42.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "BINGE & QUEUE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Playlists & Series",
                    color = Color.White,
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "Organize movie collections, series sequences and video queues.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            FocusableCard(
                onClick = { showCreateDialog = true },
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary,
                contentColor = Color(0xFF0D0F0E),
                focusedContentColor = Color(0xFF0D0F0E)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                    Text("New Series", color = Color(0xFF0D0F0E), fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 2. Main Content: Master-Detail
        if (playlists.isEmpty()) {
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
                        Icon(Icons.Default.PlaylistPlay, null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    }
                    Text(
                        text = "No series or playlists yet",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your first series or queue episodes for seamless back-to-back playback.",
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
                // Left Column: Playlist Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists, key = { it.playlist.id }) { item ->
                        val isSelected = activePlaylist?.playlist?.id == item.playlist.id
                        PlaylistMasterCard(
                            playlistWithItems = item,
                            isSelected = isSelected,
                            onClick = { selectedPlaylistId = item.playlist.id },
                            onDelete = { playlistToDelete = item }
                        )
                    }
                }

                // Right Column: Episodes / Items List
                if (activePlaylist != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceDark.copy(alpha = 0.8f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header of Active Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activePlaylist.playlist.name,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${activePlaylist.items.size} episodes in sequence",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (activePlaylist.items.isNotEmpty()) {
                                        FocusableCard(
                                            onClick = { onPlayAll(activePlaylist) },
                                            containerColor = AccentPrimary,
                                            focusedContainerColor = AccentSecondary,
                                            contentColor = Color(0xFF0D0F0E),
                                            focusedContentColor = Color(0xFF0D0F0E)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(16.dp))
                                                Text("Play All", color = Color(0xFF0D0F0E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    FocusableCard(
                                        onClick = { showStoragePickerPlaylistId = activePlaylist.playlist.id },
                                        containerColor = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("+ Storage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Items List
                            if (activePlaylist.items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No episodes added yet. Add files from TV storage or downloads.",
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activePlaylist.items, key = { it.id }) { item ->
                                        PlaylistItemRow(
                                            item = item,
                                            onClick = { onPlayItem(item, activePlaylist.items) },
                                            onRemove = { itemToRemove = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            }
        )
    }

    playlistToDelete?.let { target ->
        DeletePlaylistConfirmDialog(
            playlistName = target.playlist.name,
            onDismiss = { playlistToDelete = null },
            onConfirmDelete = {
                onDeletePlaylist(target.playlist.id)
                playlistToDelete = null
            }
        )
    }

    itemToRemove?.let { item ->
        RemovePlaylistItemConfirmDialog(
            itemTitle = item.title,
            onDismiss = { itemToRemove = null },
            onConfirmRemove = {
                onRemoveItemFromPlaylist(item.id)
                itemToRemove = null
            }
        )
    }

    showStoragePickerPlaylistId?.let { playlistId ->
        val playlist = playlists.firstOrNull { it.playlist.id == playlistId }
        SelectStorageFilesForPlaylistDialog(
            playlistName = playlist?.playlist?.name ?: "Series",
            files = archiveFiles,
            onDismiss = { showStoragePickerPlaylistId = null },
            onAddFileToPlaylist = { title, uri ->
                val file = archiveFiles.firstOrNull { it.path == uri }
                if (file != null) {
                    onAddItemsToPlaylist(playlistId, listOf(file))
                }
            }
        )
    }
}

@Composable
private fun PlaylistMasterCard(
    playlistWithItems: PlaylistWithItems,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onDelete,
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
                    Icon(Icons.Default.PlaylistPlay, null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = playlistWithItems.playlist.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlistWithItems.items.size} episodes",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            FocusableCard(
                onClick = onDelete,
                containerColor = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.size(34.dp),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DeleteOutline, null, tint = AccentRose, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    item: PlaylistItemEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onRemove,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White.copy(alpha = 0.04f),
        focusedContainerColor = CardDark,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.orderIndex + 1}",
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FocusableCard(
                onClick = onRemove,
                containerColor = Color.Transparent,
                modifier = Modifier.size(30.dp),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
