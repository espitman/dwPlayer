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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun NetworkSharesScreen(
    // SMB
    smbShares: List<SmbShareEntity>,
    currentSmbShare: SmbShareEntity?,
    currentSmbPath: String,
    smbItems: List<SmbItem>,
    isSmbLoading: Boolean,
    smbError: String?,
    onSelectSmbShare: (SmbShareEntity) -> Unit,
    onNavigateSmbPath: (String) -> Unit,
    onBackSmbPath: () -> Unit,
    onPlaySmbFile: (SmbShareEntity, String, String) -> Unit,
    onDownloadSmbFile: (SmbShareEntity, SmbItem) -> Unit,
    onAddSmbShare: (String, String, String, String?, String?, String?, (Boolean, String) -> Unit) -> Unit,
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
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAddSmbDialog by remember { mutableStateOf(false) }
    var showAddWebDavDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
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
            errorMessage = smbError,
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

    val phoneServer = webDavServers.firstOrNull { it.isAutoDiscovered }
    val discoveredPhone = discoveredServers.firstOrNull {
        it.deviceType.contains("phone", ignoreCase = true) ||
            it.serviceType.contains("dw", ignoreCase = true)
    } ?: discoveredServers.firstOrNull()
    val smbShare = smbShares.firstOrNull()
    val webDavServer = webDavServers.firstOrNull { !it.isAutoDiscovered }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(1_500)
            isScanning = false
        }
    }

    // Main Hub: mirrors the source web prototype while keeping the existing actions.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x1A234A3A), Color.Transparent),
                    center = Offset(760f, 10f),
                    radius = 620f
                )
            )
            .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.widthIn(max = 610.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
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
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "Browse devices and shared folders on your home network without moving files to the TV.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            FocusableCard(
                onClick = { isScanning = true },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(15.dp),
                containerColor = AccentPrimary,
                focusedContainerColor = AccentSecondary,
                contentColor = Color(0xFF0D0F0E),
                focusedContentColor = Color(0xFF0D0F0E),
                borderColor = Color.Transparent,
                focusedBorderColor = Color.White,
                scale = 1.04f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isScanning) Icons.Default.Sync else Icons.Default.WifiFind,
                        null,
                        tint = Color(0xFF0D0F0E),
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        if (isScanning) "Scanning…" else "Scan network",
                        color = Color(0xFF0D0F0E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NetworkPrototypeCard(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                title = phoneServer?.name ?: discoveredPhone?.serviceName ?: "My phone",
                description = "Videos shared from dwPlayer companion app",
                meta = when {
                    phoneServer != null -> "HTTP · ${phoneServer.serverUrl.removePrefix("http://").removePrefix("https://").uppercase()}"
                    discoveredPhone != null -> "HTTP · ${discoveredPhone.host}:${discoveredPhone.port} · DISCOVERED"
                    else -> "DWPLAYER COMPANION · NOT FOUND"
                },
                icon = Icons.Default.PhoneAndroid,
                isOnline = phoneServer != null || discoveredPhone != null,
                large = true,
                onClick = {
                    when {
                        phoneServer != null -> onSelectWebDavServer(phoneServer)
                        discoveredPhone != null -> onAddDiscoveredServer(discoveredPhone)
                        else -> showAddWebDavDialog = true
                    }
                },
                onLongClick = phoneServer?.let { server -> { actionWebDavServer = server } }
            )

            Column(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NetworkPrototypeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = smbShare?.name ?: "Home NAS",
                    description = "Shared media folders over SMB",
                    meta = if (smbShares.isEmpty()) {
                        "SMB · NOT CONFIGURED"
                    } else {
                        "SMB · ${smbShares.size} ${if (smbShares.size == 1) "SHARE" else "SHARES"}"
                    },
                    icon = Icons.Default.Storage,
                    isOnline = smbShare != null,
                    onClick = {
                        if (smbShare != null) onSelectSmbShare(smbShare) else showAddSmbDialog = true
                    },
                    onLongClick = smbShare?.let { share -> { actionSmbShare = share } }
                )

                NetworkPrototypeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = "Add source",
                    description = "Connect a new SMB or WebDAV server",
                    meta = "MANUAL SETUP",
                    icon = Icons.Default.Add,
                    isOnline = false,
                    onClick = { showAddOptionsDialog = true }
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NetworkPrototypeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = webDavServer?.name ?: "WebDAV",
                    description = "Your private remote archive",
                    meta = if (webDavServer == null) {
                        "HTTPS · NOT CONFIGURED"
                    } else {
                        "${if (webDavServer.serverUrl.startsWith("https", true)) "HTTPS" else "HTTP"} · ${webDavServers.count { !it.isAutoDiscovered }} ${if (webDavServers.count { !it.isAutoDiscovered } == 1) "SERVER" else "SERVERS"}"
                    },
                    icon = Icons.Default.CloudQueue,
                    isOnline = webDavServer != null,
                    onClick = {
                        if (webDavServer != null) onSelectWebDavServer(webDavServer) else showAddWebDavDialog = true
                    },
                    onLongClick = webDavServer?.let { server -> { actionWebDavServer = server } }
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
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
            onConfirm = onAddSmbShare,
            onSaved = { showAddSmbDialog = false }
        )
    }
}

@Composable
private fun NetworkPrototypeCard(
    modifier: Modifier,
    title: String,
    description: String,
    meta: String,
    icon: ImageVector,
    isOnline: Boolean,
    large: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        containerColor = SurfaceDark.copy(alpha = 0.82f),
        focusedContainerColor = CardDark,
        borderColor = Color.White.copy(alpha = 0.11f),
        focusedBorderColor = AccentPrimary,
        scale = 1.025f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (large) 28.dp else 16.dp),
            verticalArrangement = if (large) Arrangement.Top else Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (large) 58.dp else 42.dp)
                        .clip(RoundedCornerShape(if (large) 18.dp else 15.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(if (large) 18.dp else 15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = if (title == "Add source") AccentPrimary else TextSecondary,
                        modifier = Modifier.size(if (large) 28.dp else 21.dp)
                    )
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

            if (large) {
                Spacer(Modifier.height(26.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(if (large) 6.dp else 2.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = if (large) 28.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = if (large) 14.sp else 11.sp,
                    lineHeight = if (large) 19.sp else 14.sp,
                    maxLines = if (large) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(if (large) 8.dp else 1.dp))
                Text(
                    text = meta,
                    color = TextTertiary,
                    fontSize = if (large) 10.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
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
            .padding(start = 42.dp, end = 42.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NetworkBrowserHeader(
            kicker = "WEBDAV · REMOTE STORAGE",
            title = server.name,
            path = if (path.isBlank() || path == "/") "ROOT DIRECTORY" else path,
            actionLabel = if (path.isBlank() || path == "/") "Exit server" else "Up one level",
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark.copy(alpha = 0.54f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            when {
                isLoading -> NetworkBrowserState(
                    icon = Icons.Default.CloudSync,
                    title = "Loading files",
                    description = "Connecting to ${server.name} and reading this folder."
                )
                errorMessage != null -> NetworkBrowserState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Could not open this folder",
                    description = errorMessage,
                    accent = AccentRose
                )
                items.isEmpty() -> NetworkBrowserState(
                    icon = Icons.Default.FolderOff,
                    title = "This folder is empty",
                    description = "No folders or playable media were found here."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.path }) { item ->
                        NetworkBrowserRow(
                            name = item.name,
                            meta = if (item.isDirectory) "FOLDER" else item.formattedSize,
                            isDirectory = item.isDirectory,
                            isPlayable = item.isVideo,
                            badge = if (item.isDirectory) "Browse" else if (item.isVideo) "Stream" else "File",
                            onClick = {
                                if (item.isDirectory) onNavigate(item.path)
                                else if (item.isVideo) onPlay(item)
                            },
                            onLongClick = if (item.isVideo) ({ actionItem = item }) else null
                        )
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
    errorMessage: String?,
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
            .padding(start = 42.dp, end = 42.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NetworkBrowserHeader(
            kicker = "SMB · NETWORK STORAGE",
            title = share.name,
            path = if (path.isBlank() || path == "/") "ROOT DIRECTORY" else path,
            actionLabel = if (path.isBlank() || path == "/") "Exit share" else "Up one level",
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark.copy(alpha = 0.54f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            when {
                isLoading -> NetworkBrowserState(
                    icon = Icons.Default.CloudSync,
                    title = "Loading files",
                    description = "Connecting to ${share.name} and reading this folder."
                )
                errorMessage != null -> NetworkBrowserState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Could not open this folder",
                    description = errorMessage,
                    accent = AccentRose
                )
                items.isEmpty() -> NetworkBrowserState(
                    icon = Icons.Default.FolderOff,
                    title = "This folder is empty",
                    description = "No folders or playable media were found here."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.path }) { item ->
                        val isVideo = !item.isDirectory && item.name.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v")
                        NetworkBrowserRow(
                            name = item.name,
                            meta = if (item.isDirectory) "FOLDER" else formatNetworkBytes(item.size),
                            isDirectory = item.isDirectory,
                            isPlayable = isVideo,
                            badge = if (item.isDirectory) "Browse" else if (isVideo) "Play SMB" else "File",
                            onClick = {
                                if (item.isDirectory) onNavigate(item.path)
                                else if (isVideo) onPlay(item.path, item.name)
                            },
                            onLongClick = if (isVideo) ({ actionItem = item }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkBrowserHeader(
    kicker: String,
    title: String,
    path: String,
    actionLabel: String,
    onBack: () -> Unit
) {
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(title, path) {
        delay(120)
        runCatching { backFocusRequester.requestFocus() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = kicker,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = path,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FocusableCard(
            onClick = onBack,
            modifier = Modifier
                .height(50.dp)
                .focusRequester(backFocusRequester),
            shape = RoundedCornerShape(15.dp),
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = CardDark,
            borderColor = Color.White.copy(alpha = 0.16f),
            focusedBorderColor = AccentPrimary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Text(actionLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NetworkBrowserState(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color = TextSecondary
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.055f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                text = description,
                color = if (accent == AccentRose) AccentRose else TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 520.dp)
            )
        }
    }
}

@Composable
private fun NetworkBrowserRow(
    name: String,
    meta: String,
    isDirectory: Boolean,
    isPlayable: Boolean,
    badge: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    FocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = SurfaceDark.copy(alpha = 0.82f),
        focusedContainerColor = CardDark,
        borderColor = Color.Transparent,
        focusedBorderColor = AccentPrimary,
        scale = 1.008f
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.055f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Movie,
                        contentDescription = null,
                        tint = if (isPlayable) AccentPrimary else TextSecondary,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = meta,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = badge,
                    color = if (isPlayable) AccentPrimary else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatNetworkBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val unitIndex = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(1024.0))
        .toInt()
        .coerceIn(0, units.lastIndex)
    val value = bytes / Math.pow(1024.0, unitIndex.toDouble())
    return String.format("%.1f %s", value, units[unitIndex])
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
    onConfirm: (String, String, String, String?, String?, String?, (Boolean, String) -> Unit) -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                errorMessage?.let {
                    Text(it, color = AccentRose, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                            if (!isTesting && name.isNotBlank() && host.isNotBlank() && share.isNotBlank()) {
                                isTesting = true
                                errorMessage = null
                                onConfirm(
                                    name,
                                    host,
                                    share,
                                    username.takeIf { it.isNotBlank() },
                                    password.takeIf { it.isNotBlank() },
                                    null
                                ) { success, message ->
                                    isTesting = false
                                    if (success) onSaved() else errorMessage = message
                                }
                            }
                        },
                        containerColor = AccentPrimary,
                        focusedContainerColor = AccentSecondary,
                        contentColor = Color(0xFF0D0F0E),
                        focusedContentColor = Color(0xFF0D0F0E)
                    ) {
                        Text(if (isTesting) "Testing..." else "Connect & Save", color = Color(0xFF0D0F0E), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
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
