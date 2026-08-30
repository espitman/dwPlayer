@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dwplayer.data.entities.PlaylistItemEntity
import com.dwplayer.data.entities.PlaylistWithItems
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.ui.components.*
import com.dwplayer.ui.theme.*

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithItems>,
    archiveFiles: List<LocalArchiveFile> = emptyList(),
    onCreatePlaylist: (name: String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDeletePlaylistItem: (String) -> Unit,
    onAddPlaylistItem: (playlistId: String, title: String, uri: String) -> Unit,
    onPlayPlaylistItem: (playlistId: String, item: PlaylistItemEntity) -> Unit,
    onOpenAddDialog: (initialPlaylistId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddStorageFilesDialog by remember { mutableStateOf(false) }
    var confirmDeletePlaylist by remember { mutableStateOf<PlaylistWithItems?>(null) }
    var confirmDeletePlaylistItem by remember { mutableStateOf<PlaylistItemEntity?>(null) }

    val activePlaylist = remember(playlists, selectedPlaylistId) {
        if (selectedPlaylistId != null) {
            playlists.find { it.playlist.id == selectedPlaylistId }
        } else if (playlists.isNotEmpty()) {
            playlists.first()
        } else null
    }

    LaunchedEffect(playlists) {
        if (selectedPlaylistId == null && playlists.isNotEmpty()) {
            selectedPlaylistId = playlists.first().playlist.id
        } else if (playlists.isNotEmpty() && playlists.none { it.playlist.id == selectedPlaylistId }) {
            selectedPlaylistId = playlists.first().playlist.id
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
            }
        )
    }

    // Add Storage / Archive files to playlist dialog
    if (showAddStorageFilesDialog && activePlaylist != null) {
        SelectStorageFilesForPlaylistDialog(
            playlistName = activePlaylist.playlist.name,
            files = archiveFiles,
            onDismiss = { showAddStorageFilesDialog = false },
            onAddFileToPlaylist = { title, uri ->
                onAddPlaylistItem(activePlaylist.playlist.id, title, uri)
            }
        )
    }

    // Delete Playlist Confirmation Dialog
    if (confirmDeletePlaylist != null) {
        DeletePlaylistConfirmDialog(
            playlistName = confirmDeletePlaylist!!.playlist.name,
            onDismiss = { confirmDeletePlaylist = null },
            onConfirmDelete = {
                val id = confirmDeletePlaylist!!.playlist.id
                confirmDeletePlaylist = null
                onDeletePlaylist(id)
            }
        )
    }

    // Delete Item Confirmation Dialog
    if (confirmDeletePlaylistItem != null) {
        val target = confirmDeletePlaylistItem!!
        Dialog(
            onDismissRequest = { confirmDeletePlaylistItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Remove Episode?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Remove \"${target.title}\" from this playlist? The original file will not be deleted.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FocusableCard(
                            onClick = { confirmDeletePlaylistItem = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        FocusableCard(
                            onClick = {
                                val id = target.id
                                confirmDeletePlaylistItem = null
                                onDeletePlaylistItem(id)
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            containerColor = AccentRose.copy(alpha = 0.85f),
                            focusedContainerColor = AccentRose
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Remove", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PLAYLISTS & SERIES",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Organize movie collections, series sequences and video queues",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // New Playlist Button
                FocusableCard(
                    onClick = { showCreateDialog = true },
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("New Series", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Add Download Button
                FocusableCard(
                    onClick = { onOpenAddDialog(activePlaylist?.playlist?.id) },
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = AccentEmerald
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Download Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "No Playlists or Series yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create a series to auto-play episodes sequentially, or add videos directly from TV storage.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FocusableCard(
                        onClick = { showCreateDialog = true },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("+ Create First Playlist / Series", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Split Master-Detail: Playlists on Left, Episodes on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Playlists List
                LazyColumn(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(playlists, key = { _, p -> p.playlist.id }) { _, pwi ->
                        val isSelected = pwi.playlist.id == activePlaylist?.playlist?.id
                        FocusableCard(
                            onClick = { selectedPlaylistId = pwi.playlist.id },
                            onLongClick = { confirmDeletePlaylist = pwi },
                            containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.25f) else CardDark,
                            focusedContainerColor = CardDark.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlaylistPlay, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = pwi.playlist.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pwi.items.size} episodes • Hold for options",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Episodes in Selected Playlist
                if (activePlaylist != null) {
                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark.copy(alpha = 0.7f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Detail Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = activePlaylist.playlist.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activePlaylist.items.size} episodes • Auto-advances sequentially",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Add Videos from Storage button
                                FocusableCard(
                                    onClick = { showAddStorageFilesDialog = true },
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    focusedContainerColor = AccentCyan,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("+ Storage", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Play All button
                                if (activePlaylist.items.isNotEmpty()) {
                                    FocusableCard(
                                        onClick = {
                                            onPlayPlaylistItem(activePlaylist.playlist.id, activePlaylist.sortedItems.first())
                                        },
                                        containerColor = AccentEmerald,
                                        focusedContainerColor = Color(0xFF059669),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Play All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Delete Series Button
                                FocusableCard(
                                    onClick = { confirmDeletePlaylist = activePlaylist },
                                    containerColor = Color.Red.copy(alpha = 0.15f),
                                    focusedContainerColor = AccentRose,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DeleteOutline, "Delete Series", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Episodes List
                        if (activePlaylist.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "No episodes in this playlist yet.",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FocusableCard(
                                            onClick = { showAddStorageFilesDialog = true },
                                            containerColor = AccentPrimary
                                        ) {
                                            Text(
                                                "+ Add Videos from Storage",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                        FocusableCard(
                                            onClick = { onOpenAddDialog(activePlaylist.playlist.id) },
                                            containerColor = Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                "+ Add Download Link",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(activePlaylist.sortedItems, key = { _, it -> it.id }) { index, item ->
                                    FocusableCard(
                                        onClick = {
                                            onPlayPlaylistItem(activePlaylist.playlist.id, item)
                                        },
                                        onLongClick = {
                                            confirmDeletePlaylistItem = item
                                        },
                                        containerColor = CardDark,
                                        focusedContainerColor = CardDark.copy(alpha = 0.95f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(AccentPrimary.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "#${index + 1}",
                                                        color = AccentCyan,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = "Play Episode",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(22.dp)
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
        }
    }
}
