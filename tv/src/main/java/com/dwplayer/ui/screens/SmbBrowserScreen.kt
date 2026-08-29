@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.tv.material3.Text
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.models.SmbItem
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*

@Composable
fun SmbBrowserScreen(
    shares: List<SmbShareEntity>,
    currentShare: SmbShareEntity?,
    currentPath: String,
    items: List<SmbItem>,
    isLoading: Boolean,
    onSelectShare: (SmbShareEntity) -> Unit,
    onNavigatePath: (String) -> Unit,
    onBackPath: () -> Unit,
    onPlaySmbFile: (SmbShareEntity, String, String) -> Unit,
    onDownloadSmbFile: (SmbShareEntity, SmbItem) -> Unit,
    onAddShare: (String, String, String, String?, String?, String?) -> Unit,
    onDeleteShare: (String) -> Unit
) {
    var showAddShareDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NETWORK STORAGE (SMB)",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (currentShare == null) "Connect to PC, Mac, Linux or NAS storage" else "Browsing smb://${currentShare.host}/${currentShare.shareName}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (currentShare == null) {
                FocusableCard(
                    onClick = { showAddShareDialog = true },
                    containerColor = AccentPrimary,
                    focusedContainerColor = AccentSecondary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Add New Server", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                FocusableCard(
                    onClick = onBackPath,
                    containerColor = Color.White.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Back / Up Directory", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // View: Shares List or Directory Items
        if (currentShare == null) {
            if (shares.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FolderShared, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                        Text("No SMB shares added yet", color = TextSecondary, fontSize = 14.sp)
                        FocusableCard(
                            onClick = { showAddShareDialog = true },
                            containerColor = AccentPrimary
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add SMB Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shares, key = { it.id }) { share ->
                        SmbShareRow(
                            share = share,
                            onConnect = { onSelectShare(share) },
                            onDelete = { onDeleteShare(share.id) }
                        )
                    }
                }
            }
        } else {
            // Browsing SMB folder
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading folder contents...", color = AccentSecondary, fontSize = 14.sp)
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This folder is empty", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 2.dp, end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.path }) { item ->
                        SmbItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    onNavigatePath(item.path)
                                } else {
                                    onPlaySmbFile(currentShare, item.path, item.name)
                                }
                            },
                            onDownload = {
                                onDownloadSmbFile(currentShare, item)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddShareDialog) {
        AddSmbShareDialog(
            onDismiss = { showAddShareDialog = false },
            onSave = { name, host, shareName, user, pass, domain ->
                onAddShare(name, host, shareName, user, pass, domain)
                showAddShareDialog = false
            }
        )
    }
}

@Composable
private fun SmbShareRow(
    share: SmbShareEntity,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    FocusableCard(
        onClick = onConnect,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FolderShared, null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(share.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("smb://${share.host}/${share.shareName} • User: ${share.username ?: "Anonymous"}", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusableCard(
                    onClick = onDelete,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = AccentRose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmbItemRow(
    item: SmbItem,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val isVideo = item.name.endsWith(".mp4", true) ||
            item.name.endsWith(".mkv", true) ||
            item.name.endsWith(".avi", true) ||
            item.name.endsWith(".mov", true) ||
            item.name.endsWith(".ts", true)

    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else if (isVideo) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (item.isDirectory) AccentAmber else if (isVideo) AccentEmerald else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (!item.isDirectory && item.size > 0) {
                        Text(formatBytes(item.size), color = TextTertiary, fontSize = 10.sp)
                    }
                }
            }

            if (!item.isDirectory && isVideo) {
                FocusableCard(
                    onClick = onDownload,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = AccentPrimary,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Download to TV", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSmbShareDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add SMB / Windows Share", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

                SmbInputField("Display Name", name, "e.g. Living Room PC") { name = it }
                SmbInputField("Host / IP Address *", host, "e.g. 192.168.1.50") { host = it }
                SmbInputField("Share Name *", shareName, "e.g. Movies") { shareName = it }
                SmbInputField("Username (Optional)", user, "e.g. guest") { user = it }
                SmbInputField("Password (Optional)", pass, "••••••••") { pass = it }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableCard(
                        onClick = {
                            if (host.isNotBlank() && shareName.isNotBlank()) {
                                onSave(
                                    name.ifBlank { "$host/$shareName" },
                                    host.trim(),
                                    shareName.trim(),
                                    user.trim().ifEmpty { null },
                                    pass.trim().ifEmpty { null },
                                    domain.trim().ifEmpty { null }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        containerColor = AccentPrimary
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Save Server", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = TextTertiary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmbInputField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(BgDark, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(AccentPrimary),
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(placeholder, color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, sizes.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, i.toDouble()), sizes[i])
}
