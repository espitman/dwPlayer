@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.entities.WebDavServerEntity
import com.dwplayer.data.models.DiscoveredServerDto
import com.dwplayer.data.models.SmbItem
import com.dwplayer.data.models.WebDavItem
import com.dwplayer.ui.components.FocusableCard
import com.dwplayer.ui.theme.*
import kotlinx.coroutines.delay

enum class NetworkTab {
    ALL, WEBDAV, SMB, DISCOVERY
}

@Composable
fun NetworkSharesScreen(
    // SMB
    smbShares: List<SmbShareEntity>,
    currentSmbShare: SmbShareEntity?,
    currentSmbPath: String,
    smbItems: List<SmbItem>,
    isSmbLoading: Boolean,
    onSelectSmbShare: (SmbShareEntity) -> Unit,
    onNavigateSmbPath: (String) -> Unit,
    onBackSmbPath: () -> Unit,
    onPlaySmbFile: (SmbShareEntity, String, String) -> Unit,
    onDownloadSmbFile: (SmbShareEntity, SmbItem) -> Unit,
    onAddSmbShare: (String, String, String, String?, String?, String?) -> Unit,
    onDeleteSmbShare: (String) -> Unit,

    // WebDAV
    webDavServers: List<WebDavServerEntity>,
    currentWebDavServer: WebDavServerEntity?,
    currentWebDavPath: String,
    webDavItems: List<WebDavItem>,
    isWebDavLoading: Boolean,
    webDavError: String?,
    onSelectWebDavServer: (WebDavServerEntity) -> Unit,
    onNavigateWebDavPath: (String) -> Unit,
    onBackWebDavPath: () -> Unit,
    onPlayWebDavFile: (WebDavServerEntity, WebDavItem) -> Unit,
    onDownloadWebDavFile: (WebDavServerEntity, WebDavItem) -> Unit,
    onAddWebDavServer: (String, String, String?, String?, (Boolean, String) -> Unit) -> Unit,
    onDeleteWebDavServer: (String) -> Unit,

    // Discovered
    discoveredServers: List<DiscoveredServerDto>,
    onAddDiscoveredServer: (DiscoveredServerDto) -> Unit
) {
    var selectedTab by remember { mutableStateOf(NetworkTab.ALL) }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAddSmbDialog by remember { mutableStateOf(false) }
    var showAddWebDavDialog by remember { mutableStateOf(false) }
    var actionWebDavServer by remember { mutableStateOf<WebDavServerEntity?>(null) }
    var actionSmbShare by remember { mutableStateOf<SmbShareEntity?>(null) }
    var confirmDeleteWebDav by remember { mutableStateOf<WebDavServerEntity?>(null) }
    var confirmDeleteSmb by remember { mutableStateOf<SmbShareEntity?>(null) }

    // If actively browsing an SMB share
    if (currentSmbShare != null) {
        SmbBrowserView(
            share = currentSmbShare,
            path = currentSmbPath,
            items = smbItems,
            isLoading = isSmbLoading,
            onNavigate = onNavigateSmbPath,
            onBack = onBackSmbPath,
            onPlay = { path, name -> onPlaySmbFile(currentSmbShare, path, name) },
            onDownload = { item -> onDownloadSmbFile(currentSmbShare, item) }
        )
        return
    }

    // If actively browsing a WebDAV server
    if (currentWebDavServer != null) {
        WebDavBrowserView(
            server = currentWebDavServer,
            path = currentWebDavPath,
            items = webDavItems,
            isLoading = isWebDavLoading,
            errorMessage = webDavError,
            onNavigate = onNavigateWebDavPath,
            onBack = onBackWebDavPath,
            onPlay = { item -> onPlayWebDavFile(currentWebDavServer, item) },
            onDownload = { item -> onDownloadWebDavFile(currentWebDavServer, item) }
        )
        return
    }

    // Main Hub: Modern Source Grid
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LOCAL & REMOTE SOURCES",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Network",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Browse devices and shared folders on your home network without moving files to the TV.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            FocusableCard(
                onClick = { showAddOptionsDialog = true },
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
                    Text("Add Source", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Protocol Filter Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tabs = listOf(
                NetworkTab.ALL to "All Sources",
                NetworkTab.WEBDAV to "WebDAV & Phone (${webDavServers.size})",
                NetworkTab.SMB to "SMB Shares (${smbShares.size})",
                NetworkTab.DISCOVERY to "Discovered (${discoveredServers.size})"
            )

            tabs.forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                FocusableCard(
                    onClick = { selectedTab = tab },
                    shape = RoundedCornerShape(10.dp),
                    containerColor = if (isSelected) Color.White else Color.Transparent,
                    focusedContainerColor = if (isSelected) Color.White else CardDark
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Grid Content
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            // WebDAV Servers
            if (selectedTab == NetworkTab.ALL || selectedTab == NetworkTab.WEBDAV) {
                items(webDavServers, key = { "webdav_${it.id}" }) { server ->
                    NetworkSourceCard(
                        title = server.name,
                        subtitle = server.serverUrl,
                        protocol = if (server.isAutoDiscovered) "PHONE" else "WEBDAV",
                        icon = if (server.isAutoDiscovered) Icons.Default.PhoneAndroid else Icons.Default.CloudQueue,
                        isOnline = true,
                        onClick = { onSelectWebDavServer(server) },
                        onLongClick = { actionWebDavServer = server }
                    )
                }
            }

            // SMB Shares
            if (selectedTab == NetworkTab.ALL || selectedTab == NetworkTab.SMB) {
                items(smbShares, key = { "smb_${it.id}" }) { share ->
                    NetworkSourceCard(
                        title = share.name,
                        subtitle = "smb://${share.host}/${share.shareName}",
                        protocol = "SMB",
                        icon = Icons.Default.FolderShared,
                        isOnline = true,
                        onClick = { onSelectSmbShare(share) },
                        onLongClick = { actionSmbShare = share }
                    )
                }
            }

            // Discovered Servers
            if (selectedTab == NetworkTab.ALL || selectedTab == NetworkTab.DISCOVERY) {
                items(discoveredServers, key = { "disc_${it.host}_${it.port}" }) { item ->
                    NetworkSourceCard(
                        title = item.serviceName,
                        subtitle = item.url,
                        protocol = item.deviceType.uppercase(),
                        icon = Icons.Default.WifiTethering,
                        isOnline = true,
                        onClick = { onAddDiscoveredServer(item) },
                        onLongClick = { onAddDiscoveredServer(item) }
                    )
                }
            }

            // Add New Source Tile
            item {
                FocusableCard(
                    onClick = { showAddOptionsDialog = true },
                    modifier = Modifier.height(130.dp),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = CardDark.copy(alpha = 0.4f),
                    focusedContainerColor = CardDark
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Add Source", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Connect a new SMB or WebDAV server", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Add Options Modal
    if (showAddOptionsDialog) {
        Dialog(onDismissRequest = { showAddOptionsDialog = false }) {
            Box(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Add Network Source", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Select the connection protocol you want to configure:", color = TextSecondary, fontSize = 12.sp)

                    FocusableCard(
                        onClick = {
                            showAddOptionsDialog = false
                            showAddWebDavDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        containerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                            Column {
                                Text("WebDAV / Phone Companion", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("HTTP/HTTPS endpoints or Android Companion", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    FocusableCard(
                        onClick = {
                            showAddOptionsDialog = false
                            showAddSmbDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        containerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.FolderShared, null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                            Column {
                                Text("SMB Network Share", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Windows, Mac, Linux, NAS (SMBv2/v3)", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    FocusableCard(
                        onClick = { showAddOptionsDialog = false },
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Action dialogs
    actionWebDavServer?.let { target ->
        NetworkServerActionDialog(
            title = target.name,
            subtitle = target.serverUrl,
            icon = if (target.isAutoDiscovered) Icons.Default.PhoneAndroid else Icons.Default.CloudQueue,
            onOpen = {
                actionWebDavServer = null
                onSelectWebDavServer(target)
            },
            onDelete = {
                actionWebDavServer = null
                confirmDeleteWebDav = target
            },
            onDismiss = { actionWebDavServer = null }
        )
    }

    actionSmbShare?.let { target ->
        NetworkServerActionDialog(
            title = target.name,
            subtitle = "smb://${target.host}/${target.shareName}",
            icon = Icons.Default.FolderShared,
            onOpen = {
                actionSmbShare = null
                onSelectSmbShare(target)
            },
            onDelete = {
                actionSmbShare = null
                confirmDeleteSmb = target
            },
            onDismiss = { actionSmbShare = null }
        )
    }

    confirmDeleteWebDav?.let { target ->
        NetworkDeleteConfirmDialog(
            title = "Delete WebDAV Server",
            itemName = target.name,
            itemDetails = target.serverUrl,
            onConfirmDelete = {
                confirmDeleteWebDav = null
                onDeleteWebDavServer(target.id)
            },
            onDismiss = { confirmDeleteWebDav = null }
        )
    }

    confirmDeleteSmb?.let { target ->
        NetworkDeleteConfirmDialog(
            title = "Delete SMB Share",
            itemName = target.name,
            itemDetails = "smb://${target.host}/${target.shareName}",
            onConfirmDelete = {
                confirmDeleteSmb = null
                onDeleteSmbShare(target.id)
            },
            onDismiss = { confirmDeleteSmb = null }
        )
    }

    if (showAddWebDavDialog) {
        AddWebDavDialog(
            onDismiss = { showAddWebDavDialog = false },
            onConfirm = { name, url, user, pass, callback ->
                onAddWebDavServer(name, url, user, pass) { success, msg ->
                    callback(success, msg)
                    if (success) showAddWebDavDialog = false
                }
            }
        )
    }

    if (showAddSmbDialog) {
        AddSmbShareDialog(
            onDismiss = { showAddSmbDialog = false },
            onConfirm = { name, host, share, user, pass, domain ->
                onAddSmbShare(name, host, share, user, pass, domain)
                showAddSmbDialog = false
            }
        )
    }
}

@Composable
private fun NetworkSourceCard(
    title: String,
    subtitle: String,
    protocol: String,
    icon: ImageVector,
    isOnline: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = CardDark.copy(alpha = 0.7f),
        focusedContainerColor = CardDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                }

                if (isOnline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Text(
                            text = "ONLINE",
                            color = AccentEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NetworkServerActionDialog(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val headerFocusRequester = remember { FocusRequester() }
    val openedAt = remember { System.currentTimeMillis() }
    val safeOpen = remember(onOpen) {
        {
            if (System.currentTimeMillis() - openedAt > 500L) {
                onOpen()
            }
        }
    }
    val safeDelete = remember(onDelete) {
        {
            if (System.currentTimeMillis() - openedAt > 500L) {
                onDelete()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            headerFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 1, fontFamily = FontFamily.Monospace)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableCard(
                        onClick = safeOpen,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary,
                        contentColor = Color(0xFF0D0F0E),
                        focusedContentColor = Color(0xFF0D0F0E)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                            Text("Open & Browse Files", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    FocusableCard(
                        onClick = safeDelete,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        containerColor = AccentRose.copy(alpha = 0.15f),
                        focusedContainerColor = AccentRose
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = AccentRose, modifier = Modifier.size(18.dp))
                            Text("Delete Connection", color = AccentRose, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        containerColor = Color.Transparent
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkDeleteConfirmDialog(
    title: String,
    itemName: String,
    itemDetails: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
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
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Remove \"$itemName\" ($itemDetails) from your TV connections?", color = TextSecondary, fontSize = 12.sp)

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
                            Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebDavBrowserView(
    server: WebDavServerEntity,
    path: String,
    items: List<WebDavItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlay: (WebDavItem) -> Unit,
    onDownload: (WebDavItem) -> Unit
) {
    var actionItem by remember { mutableStateOf<WebDavItem?>(null) }

    if (actionItem != null) {
        val target = actionItem!!
        NetworkMediaActionDialog(
            title = target.name,
            sizeText = target.formattedSize,
            onPlay = {
                actionItem = null
                onPlay(target)
            },
            onDownload = {
                actionItem = null
                onDownload(target)
            },
            onDismiss = { actionItem = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(server.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    text = if (path.isBlank() || path == "/") "Root Directory" else path,
                    color = AccentPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            FocusableCard(
                onClick = onBack,
                containerColor = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(if (path.isBlank() || path == "/") "Exit Server" else "Up One Level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading files...", color = TextSecondary, fontSize = 14.sp)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $errorMessage", color = AccentRose, fontSize = 14.sp)
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", color = TextTertiary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.path }) { item ->
                    FocusableCard(
                        onClick = {
                            if (item.isDirectory) onNavigate(item.path)
                            else if (item.isVideo) onPlay(item)
                        },
                        onLongClick = { if (item.isVideo) actionItem = item },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = CardDark.copy(alpha = 0.7f),
                        focusedContainerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) TextSecondary else AccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.formattedSize, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (item.isVideo) {
                                Text("Stream", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmbBrowserView(
    share: SmbShareEntity,
    path: String,
    items: List<SmbItem>,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit,
    onDownload: (SmbItem) -> Unit
) {
    var actionItem by remember { mutableStateOf<SmbItem?>(null) }

    if (actionItem != null) {
        val target = actionItem!!
        NetworkMediaActionDialog(
            title = target.name,
            sizeText = "${target.size / (1024 * 1024)} MB",
            onPlay = {
                actionItem = null
                onPlay(target.path, target.name)
            },
            onDownload = {
                actionItem = null
                onDownload(target)
            },
            onDismiss = { actionItem = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(share.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    text = if (path.isBlank() || path == "/") "Root Directory" else path,
                    color = AccentPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            FocusableCard(
                onClick = onBack,
                containerColor = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(if (path.isBlank() || path == "/") "Exit Share" else "Up One Level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading SMB directory...", color = TextSecondary, fontSize = 14.sp)
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", color = TextTertiary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.path }) { item ->
                    val isVideo = !item.isDirectory && item.name.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v")
                    FocusableCard(
                        onClick = {
                            if (item.isDirectory) onNavigate(item.path)
                            else if (isVideo) onPlay(item.path, item.name)
                        },
                        onLongClick = { if (isVideo) actionItem = item },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = CardDark.copy(alpha = 0.7f),
                        focusedContainerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) TextSecondary else AccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (item.isDirectory) "Folder" else "${item.size / (1024 * 1024)} MB", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (isVideo) {
                                Text("Play SMB", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkMediaActionDialog(
    title: String,
    sizeText: String,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
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
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text("Size: $sizeText", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableCard(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary,
                        contentColor = Color(0xFF0D0F0E),
                        focusedContentColor = Color(0xFF0D0F0E)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0D0F0E), modifier = Modifier.size(18.dp))
                            Text("Stream Now", color = Color(0xFF0D0F0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    FocusableCard(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Download to TV", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWebDavDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?, (Boolean, String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("http://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add WebDAV Server", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvInputRow("Name", name, "e.g. My Phone / NAS WebDAV") { name = it }
                    TvInputRow("Server URL", url, "http://192.168.1.50:8080/webdav") { url = it }
                    TvInputRow("Username (Optional)", username, "user") { username = it }
                    TvInputRow("Password (Optional)", password, "pass") { password = it }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = AccentRose, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableCard(
                        onClick = onDismiss,
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableCard(
                        onClick = {
                            if (name.isBlank() || url.isBlank()) {
                                errorMessage = "Name and URL are required"
                                return@FocusableCard
                            }
                            isTesting = true
                            errorMessage = null
                            onConfirm(name, url, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() }) { success, msg ->
                                isTesting = false
                                if (!success) errorMessage = msg
                            }
                        },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary,
                        contentColor = Color(0xFF0D0F0E),
                        focusedContentColor = Color(0xFF0D0F0E)
                    ) {
                        Text(
                            if (isTesting) "Testing..." else "Connect & Save",
                            color = Color(0xFF0D0F0E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSmbShareDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add SMB Share", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvInputRow("Name", name, "e.g. Living Room PC") { name = it }
                    TvInputRow("Host / IP", host, "192.168.1.100") { host = it }
                    TvInputRow("Share Folder Name", share, "Movies") { share = it }
                    TvInputRow("Username", username, "Optional") { username = it }
                    TvInputRow("Password", password, "Optional") { password = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableCard(onClick = onDismiss, containerColor = Color.White.copy(alpha = 0.08f)) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableCard(
                        onClick = {
                            if (name.isNotBlank() && host.isNotBlank() && share.isNotBlank()) {
                                onConfirm(name, host, share, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() }, null)
                            }
                        },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary,
                        contentColor = Color(0xFF0D0F0E),
                        focusedContentColor = Color(0xFF0D0F0E)
                    ) {
                        Text("Save Share", color = Color(0xFF0D0F0E), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TvInputRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardDark)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(AccentPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextTertiary, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}
