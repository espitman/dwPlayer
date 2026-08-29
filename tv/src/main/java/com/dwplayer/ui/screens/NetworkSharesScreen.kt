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
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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

enum class NetworkTab {
    WEBDAV, SMB, DISCOVERY
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
    var selectedTab by remember { mutableStateOf(NetworkTab.WEBDAV) }
    var showAddSmbDialog by remember { mutableStateOf(false) }
    var showAddWebDavDialog by remember { mutableStateOf(false) }

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

    // Main Hub
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NETWORK STREAMING & SHARES",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Stream smoothly from WebDAV, Android Phones, PC/Mac, or NAS",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectedTab == NetworkTab.WEBDAV) {
                    FocusableCard(
                        onClick = { showAddWebDavDialog = true },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Add WebDAV", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (selectedTab == NetworkTab.SMB) {
                    FocusableCard(
                        onClick = { showAddSmbDialog = true },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Add SMB", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Protocol Selector Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tabs = listOf(
                Triple(NetworkTab.WEBDAV, "WebDAV & Phone Servers", Icons.Default.CloudQueue),
                Triple(NetworkTab.SMB, "Windows / NAS (SMB)", Icons.Default.FolderShared),
                Triple(NetworkTab.DISCOVERY, "Discovered Devices (${discoveredServers.size})", Icons.Default.Sensors)
            )

            tabs.forEach { (tab, title, icon) ->
                val isSelected = selectedTab == tab
                FocusableCard(
                    onClick = { selectedTab = tab },
                    containerColor = if (isSelected) AccentPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = AccentPrimary.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentPrimary else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) AccentSecondary else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            NetworkTab.WEBDAV -> {
                if (webDavServers.isEmpty()) {
                    EmptyNetworkView(
                        icon = Icons.Default.CloudQueue,
                        title = "No WebDAV Servers Added",
                        subtitle = "Add a WebDAV endpoint or connect to our Android Phone companion app.",
                        buttonText = "Add WebDAV Server",
                        onButtonClick = { showAddWebDavDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(webDavServers, key = { it.id }) { server ->
                            FocusableCard(
                                onClick = { onSelectWebDavServer(server) },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = CardDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(AccentPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (server.isAutoDiscovered) Icons.Default.PhoneAndroid else Icons.Default.CloudQueue,
                                                contentDescription = null,
                                                tint = AccentSecondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = server.name,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (server.isAutoDiscovered) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Auto-Discovered", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = server.serverUrl,
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FocusableCard(
                                            onClick = { onDeleteWebDavServer(server.id) },
                                            containerColor = Color.Red.copy(alpha = 0.15f),
                                            focusedContainerColor = Color.Red.copy(alpha = 0.35f)
                                        ) {
                                            Box(modifier = Modifier.padding(8.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NetworkTab.SMB -> {
                if (smbShares.isEmpty()) {
                    EmptyNetworkView(
                        icon = Icons.Default.FolderShared,
                        title = "No SMB Shares Configured",
                        subtitle = "Connect to Windows shared folders, Mac, Linux, or NAS devices.",
                        buttonText = "Add SMB Share",
                        onButtonClick = { showAddSmbDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(smbShares, key = { it.id }) { share ->
                            FocusableCard(
                                onClick = { onSelectSmbShare(share) },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = CardDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(AccentPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = AccentSecondary, modifier = Modifier.size(24.dp))
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = share.name,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "smb://${share.host}/${share.shareName}",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FocusableCard(
                                            onClick = { onDeleteSmbShare(share.id) },
                                            containerColor = Color.Red.copy(alpha = 0.15f),
                                            focusedContainerColor = Color.Red.copy(alpha = 0.35f)
                                        ) {
                                            Box(modifier = Modifier.padding(8.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NetworkTab.DISCOVERY -> {
                if (discoveredServers.isEmpty()) {
                    EmptyNetworkView(
                        icon = Icons.Default.Sensors,
                        title = "Scanning Local Wi-Fi Network...",
                        subtitle = "Turn on the dwShare Phone app or WebDAV server on your Wi-Fi network to detect it automatically.",
                        buttonText = "Refresh Network",
                        onButtonClick = {}
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(discoveredServers, key = { "${it.host}:${it.port}" }) { item ->
                            FocusableCard(
                                onClick = { onAddDiscoveredServer(item) },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = CardDark
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = item.serviceName,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(AccentPrimary.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(item.deviceType, color = AccentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = item.url,
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Click to Connect", color = AccentSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.AddLink, contentDescription = null, tint = AccentSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Path Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = server.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (path.isBlank() || path == "/") "Root Directory" else path,
                    color = AccentSecondary,
                    fontSize = 12.sp
                )
            }

            FocusableCard(
                onClick = onBack,
                containerColor = Color.White.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(if (path.isBlank() || path == "/") "Exit Server" else "Up One Level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Connecting and loading files...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                    Text("Connection Error", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(errorMessage, color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.path }) { item ->
                    FocusableCard(
                        onClick = {
                            if (item.isDirectory) {
                                onNavigate(item.path)
                            } else if (item.isVideo) {
                                onPlay(item)
                            }
                        },
                        onLongClick = {
                            if (item.isVideo) {
                                actionItem = item
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        containerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        item.isDirectory -> Icons.Default.Folder
                                        item.isVideo -> Icons.Default.PlayCircle
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        item.isDirectory -> AccentSecondary
                                        item.isVideo -> Color(0xFF10B981)
                                        else -> Color.White.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.formattedSize,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (item.isVideo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Hold to Download", color = TextSecondary, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stream", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                            } else if (item.isDirectory) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
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
            .padding(start = 24.dp, end = 36.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = share.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(text = if (path.isBlank() || path == "/") "Root Directory" else path, color = AccentSecondary, fontSize = 12.sp)
            }

            FocusableCard(onClick = onBack, containerColor = Color.White.copy(alpha = 0.1f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(if (path.isBlank() || path == "/") "Exit Share" else "Up One Level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading SMB directory...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", color = TextSecondary, fontSize = 14.sp)
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
                            if (item.isDirectory) {
                                onNavigate(item.path)
                            } else if (isVideo) {
                                onPlay(item.path, item.name)
                            }
                        },
                        onLongClick = {
                            if (isVideo) {
                                actionItem = item
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        containerColor = CardDark
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else if (isVideo) Icons.Default.Movie else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) AccentSecondary else if (isVideo) Color(0xFF10B981) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.isDirectory) "Folder" else "${item.size / (1024 * 1024)} MB",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (isVideo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Hold to Download", color = TextSecondary, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play SMB", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
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
    val openedAt = remember { System.currentTimeMillis() }
    val safePlay = remember(onPlay) {
        {
            if (System.currentTimeMillis() - openedAt > 500L) {
                onPlay()
            }
        }
    }
    val safeDownload = remember(onDownload) {
        {
            if (System.currentTimeMillis() - openedAt > 500L) {
                onDownload()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AccentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: $sizeText",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Play / Stream Online
                    FocusableCard(
                        onClick = safePlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        containerColor = AccentPrimary.copy(alpha = 0.85f),
                        focusedContainerColor = AccentPrimary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Stream Now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Download / Transfer to TV Storage
                    FocusableCard(
                        onClick = safeDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF10B981),
                        focusedContentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            Column {
                                Text("Download to TV Storage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Save locally for offline playback", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    // Cancel
                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        containerColor = Color.White.copy(alpha = 0.06f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNetworkView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentSecondary, modifier = Modifier.size(32.dp))
            }
            Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(6.dp))
            FocusableCard(
                onClick = onButtonClick,
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(buttonText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                .width(640.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgDark)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add WebDAV Server", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Enter WebDAV URL, Android Phone dwShare IP, or NAS endpoint", color = TextSecondary, fontSize = 12.sp)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvInputRow("Name", name, "e.g. My Phone / NAS WebDAV") { name = it }
                    TvInputRow("Server URL", url, "http://192.168.1.50:8080/webdav") { url = it }
                    TvInputRow("Username (Optional)", username, "user") { username = it }
                    TvInputRow("Password (Optional)", password, "pass") { password = it }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableCard(
                        onClick = onDismiss,
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text("Cancel", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
                                if (!success) {
                                    errorMessage = msg
                                }
                            }
                        },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Text(
                            if (isTesting) "Testing..." else "Connect & Save",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                .width(640.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgDark)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add SMB Share", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
                    FocusableCard(onClick = onDismiss, containerColor = Color.White.copy(alpha = 0.1f)) {
                        Text("Cancel", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FocusableCard(
                        onClick = {
                            if (name.isNotBlank() && host.isNotBlank() && share.isNotBlank()) {
                                onConfirm(name, host, share, username.takeIf { it.isNotBlank() }, password.takeIf { it.isNotBlank() }, null)
                            }
                        },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary
                    ) {
                        Text("Save Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                cursorBrush = SolidColor(AccentSecondary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}
