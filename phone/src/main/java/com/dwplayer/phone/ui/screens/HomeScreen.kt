package com.dwplayer.phone.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.dwplayer.phone.R
import com.dwplayer.phone.core.media.MediaItem
import com.dwplayer.phone.core.tv.*
import com.dwplayer.phone.ui.viewmodel.PhoneDestination
import com.dwplayer.phone.ui.viewmodel.PhoneUiState
import com.dwplayer.phone.ui.viewmodel.PhoneViewModel

private val Bg = Color(0xFF101512)
private val Surface = Color(0xFF1B221E)
private val SurfaceDark = Color(0xFF111713)
private val Foreground = Color(0xFFF2F5EC)
private val Muted = Color(0xFFA2ACA5)
private val Border = Color(0xFF3A453E)
private val Accent = Color(0xFFACED3F)
private val Danger = Color(0xFFFF7B72)
private val ControlBorderColor = Color(0xFF3A453E)
private val ControlBorderWidth = 1.dp
private val ControlRadius = 12.dp

private data class RecentMediaEntry(
    val title: String,
    val meta: String,
    val timestamp: Long,
    val phoneMedia: MediaItem? = null,
    val tvMedia: TvArchiveFile? = null
)

@Composable
fun HomeScreen(viewModel: PhoneViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            viewModel.onFolderSelected(it, DocumentFile.fromTreeUri(context, it)?.name ?: "Selected folder")
        }
    }

    LaunchedEffect(state.messageId) { if (state.messageId > 0 && state.message != null) snackbar.showSnackbar(state.message!!) }
    BackHandler(enabled = state.destination != PhoneDestination.HOME) { viewModel.navigate(PhoneDestination.HOME) }

    Scaffold(
        containerColor = Bg,
        topBar = { AppHeader(state, viewModel) },
        bottomBar = { BottomNavigation(state.destination, viewModel::navigate) },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it, containerColor = Foreground, contentColor = Bg) } }
    ) { padding ->
        AnimatedContent(
            targetState = state.destination,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(260)) { width -> direction * width / 4 } + fadeIn(tween(210))) togetherWith
                    (slideOutHorizontally(tween(230)) { width -> -direction * width / 5 } + fadeOut(tween(170)))
            },
            label = "phone-tab-transition"
        ) { destination ->
            when (destination) {
                PhoneDestination.HOME -> HomePage(state, viewModel, padding)
                PhoneDestination.LIBRARY -> LibraryPage(state, viewModel, padding) { folderPicker.launch(null) }
                PhoneDestination.SEND -> SendPage(state, viewModel, padding)
                PhoneDestination.PLAYLISTS -> PlaylistsPage(state, viewModel, padding)
                PhoneDestination.NETWORK -> NetworkPage(state, viewModel, padding)
            }
        }
    }
}

@Composable
private fun AppHeader(state: PhoneUiState, viewModel: PhoneViewModel) {
    val loadingPulse = rememberInfiniteTransition(label = "header-loading")
    val loadingAlpha by loadingPulse.animateFloat(
        initialValue = .38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "connection-pulse"
    )
    val loading = state.isRefreshingTv || state.isLoadingMedia
    Surface(color = Bg.copy(alpha = .97f), shadowElevation = 0.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(68.dp)
                .drawBehind {
                    drawLine(
                        color = ControlBorderColor.copy(alpha = .6f),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = ControlBorderWidth.toPx()
                    )
                }
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Foreground), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = Bg, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("dwPlayer", color = Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text("MOBILE COMPANION", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.6.sp)
                    }
                }
                Row(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(11.dp)).background(Surface).border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(11.dp)).clickable {
                        if (state.tvConnected) viewModel.navigate(PhoneDestination.HOME) else viewModel.navigate(PhoneDestination.NETWORK)
                    }.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                alpha = if (loading) loadingAlpha else 1f
                                val pulseScale = if (loading) .82f + loadingAlpha * .18f else 1f
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(if (state.tvConnected) Accent else Muted)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(if (state.tvConnected) "Living Room TV" else "Connect TV", color = Foreground, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: PhoneDestination, navigate: (PhoneDestination) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(102.dp)) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(84.dp)
                .background(Bg.copy(alpha = .98f))
                .drawBehind {
                    drawLine(
                        color = Border.copy(alpha = .6f),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            NavItem(PhoneDestination.HOME, selected, "Home", Icons.Default.Home, navigate)
            NavItem(PhoneDestination.LIBRARY, selected, "Library", Icons.Default.VideoLibrary, navigate)
            Spacer(Modifier.weight(1f).fillMaxHeight())
            NavItem(PhoneDestination.PLAYLISTS, selected, "Playlists", Icons.Default.PlaylistPlay, navigate)
            NavItem(PhoneDestination.NETWORK, selected, "Network", Icons.Default.Cloud, navigate)
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(.2f)
                .padding(horizontal = 5.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Foreground)
                .border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(13.dp))
                .clickable { navigate(PhoneDestination.SEND) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Bg, modifier = Modifier.size(27.dp))
                Text(
                    "Send",
                    color = Bg,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(destination: PhoneDestination, selected: PhoneDestination, label: String, icon: ImageVector, navigate: (PhoneDestination) -> Unit) {
    val selectedProgress by animateFloatAsState(if (destination == selected) 1f else 0f, tween(180), label = "nav-selection")
    Column(
        Modifier.weight(1f).fillMaxHeight().padding(6.dp).clip(RoundedCornerShape(13.dp)).background(Surface.copy(alpha = selectedProgress)).clickable { navigate(destination) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (destination == selected) Foreground else Muted, modifier = Modifier.size(21.dp).graphicsLayer { scaleX = .94f + .06f * selectedProgress; scaleY = scaleX })
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (destination == selected) Foreground else Muted, fontSize = 9.sp)
    }
}

@Composable
private fun Page(padding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Bg).padding(padding).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        content = content
    )
}

@Composable
private fun ScreenHeading(eyebrow: String, title: String, lead: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(eyebrow, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(7.dp))
        Text(title, color = Foreground, fontSize = 38.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(9.dp))
        Text(lead, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun DwCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(19.dp)).padding(18.dp), content = content)
}

@Composable
private fun HomePage(state: PhoneUiState, viewModel: PhoneViewModel, padding: PaddingValues) {
    val recent = remember(state.mediaList, state.tvArchive) {
        buildList {
            state.mediaList.forEach { media ->
                add(RecentMediaEntry(media.title, "PHONE FOLDER · ${formatSize(media.size)}", media.dateModified, phoneMedia = media))
            }
            state.tvArchive.forEach { media ->
                add(RecentMediaEntry(media.name, "TV ARCHIVE · ${media.sizeFormatted.ifBlank { formatSize(media.sizeBytes) }}", media.lastModified, tvMedia = media))
            }
        }.sortedByDescending { it.timestamp }.take(2)
    }
    Page(padding) {
    item { ScreenHeading("CONNECTED NOW", "Your TV,\nwithin reach.", "Share local media, send downloads, and\ncontrol playback from one place.") }
    item {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Foreground).padding(20.dp)) {
            Text("LIVING ROOM TV", color = Bg.copy(alpha = .65f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(if (state.tvConnected) "Ready to play" else "Waiting for TV", color = Bg, fontSize = 29.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(if (state.tvConnected) "dwPlayer is online on your local Wi-Fi." else "Open Network to connect this phone.", color = Bg.copy(alpha = .72f), fontSize = 13.sp)
            HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Bg.copy(alpha = .18f))
            Row {
                Metric("STORAGE", state.storage?.let { "${it.freeSpace} / ${it.totalSpace} FREE" } ?: "NOT AVAILABLE", Modifier.weight(1f), dark = true)
                Metric("DOWNLOADS", if (state.downloads.summary.active == 0) "NONE ACTIVE" else "${state.downloads.summary.active} ACTIVE", Modifier.weight(1f), dark = true)
            }
        }
    }
    item {
        DwCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Label("QUICK REMOTE"); Text(state.remoteStatus.title.ifBlank { "Playback" }, color = Foreground, fontSize = 18.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, maxLines = 1) }
                Tag(if (state.remoteStatus.playerActive) "LIVE" else "READY")
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RemoteButton(Icons.Default.Replay10, Modifier.weight(1f)) { viewModel.remote("backward") }
                RemoteButton(Icons.Default.SkipPrevious, Modifier.weight(1f)) { viewModel.remote("previous") }
                RemoteButton(if (state.remoteStatus.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, Modifier.weight(1f), primary = true) { viewModel.remote("play-pause") }
                RemoteButton(Icons.Default.SkipNext, Modifier.weight(1f)) { viewModel.remote("next") }
                RemoteButton(Icons.Default.Forward10, Modifier.weight(1f)) { viewModel.remote("forward") }
            }
        }
    }
    item { SectionTitle("Quick actions") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Share a folder", "Stream phone files", Icons.Default.Folder, Modifier.weight(1f)) { viewModel.navigate(PhoneDestination.LIBRARY) }
                QuickAction("Send a link", "Download on TV", Icons.Default.Link, Modifier.weight(1f)) { viewModel.navigate(PhoneDestination.SEND) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("New playlist", "Group episodes", Icons.Default.PlaylistAdd, Modifier.weight(1f)) { viewModel.navigate(PhoneDestination.PLAYLISTS) }
                QuickAction("Add a source", "WebDAV or SMB", Icons.Default.Cloud, Modifier.weight(1f)) { viewModel.navigate(PhoneDestination.NETWORK) }
            }
        }
    }
    item { SectionTitle("Recent media", "View all") { viewModel.navigate(PhoneDestination.LIBRARY) } }
    if (recent.isEmpty()) item { EmptyCard("No recent media yet. Choose a phone folder or download a video on TV.", Icons.Default.VideoLibrary) }
    else items(recent) { media ->
        MediaRow(media.title, media.meta, if (recent.indexOf(media) % 2 == 0) R.drawable.fallback_coast else R.drawable.fallback_forest) {
            media.phoneMedia?.let(viewModel::playPhoneMedia) ?: media.tvMedia?.let(viewModel::playTvArchive)
        }
    }
}
}

@Composable private fun RemoteButton(icon: ImageVector, modifier: Modifier, primary: Boolean = false, action: () -> Unit) {
    Box(modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (primary) Foreground else SurfaceDark).border(1.dp, if (primary) Foreground else Border, RoundedCornerShape(12.dp)).clickable(onClick = action), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (primary) Bg else Muted)
    }
}

@Composable private fun QuickAction(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, action: () -> Unit) {
    Column(modifier.height(112.dp).clip(RoundedCornerShape(17.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(17.dp)).clickable(onClick = action).padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Icon(icon, null, tint = Muted)
        Column { Text(title, color = Foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun LibraryPage(state: PhoneUiState, viewModel: PhoneViewModel, padding: PaddingValues, chooseFolder: () -> Unit) = Page(padding) {
    item { ScreenHeading("ON THIS PHONE", "Local library", "Choose one folder and share only its videos with your TV.") }
    item {
        DwCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column { Label("SHARED FOLDER"); Text(state.selectedFolderName, color = Foreground, fontSize = 23.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) }
                SmallButton(if (state.hasSelectedFolder) "Change" else "Choose", chooseFolder)
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(13.dp)).background(SurfaceDark).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = Muted); Spacer(Modifier.width(12.dp)); Column { Text(state.selectedFolderName, color = Foreground, fontWeight = FontWeight.Bold); Text("${state.mediaList.size} VIDEO${if (state.mediaList.size == 1) "" else "S"} AVAILABLE", color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
            }
        }
    }
    item {
        DwCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.isServerRunning) Accent else Muted)); Spacer(Modifier.width(10.dp)); Column { Label("PHONE SERVER"); Text(if (state.isServerRunning) "Running" else "Stopped", color = Foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) } }
                Switch(checked = state.isServerRunning, onCheckedChange = viewModel::toggleServer, colors = SwitchDefaults.colors(checkedTrackColor = Foreground, checkedThumbColor = Bg, uncheckedTrackColor = SurfaceDark, uncheckedBorderColor = Border))
            }
            Text(if (state.isServerRunning) "Broadcasting videos from the shared folder to TV." else "Turn on to stream videos from this phone.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp))
            if (state.isServerRunning) {
                HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Border)
                val context = LocalContext.current
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.serverUrl, color = Foreground, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("dwShare URL", state.serverUrl)) }) { Icon(Icons.Default.ContentCopy, "Copy URL", tint = Muted) }
                }
                state.qrBitmap?.let { qr ->
                    Image(qr.asImageBitmap(), "Local stream QR", Modifier.padding(top = 18.dp).size(116.dp).clip(RoundedCornerShape(15.dp)).background(Foreground).padding(10.dp).align(Alignment.CenterHorizontally))
                    Text("Scan to browse or stream on a computer", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                }
            }
        }
    }
    item { SectionTitle("Videos in ${state.selectedFolderName}", "${state.mediaList.size} ITEM${if (state.mediaList.size == 1) "" else "S"}") }
    if (state.mediaList.isEmpty()) item { EmptyCard(if (state.hasSelectedFolder) "No video files found in this folder." else "Choose a folder to start sharing videos.", Icons.Default.VideoLibrary) }
    else items(state.mediaList) { media -> MediaRow(media.title, "${media.mimeType.substringAfter('/').uppercase()} · ${formatSize(media.size)}", if (state.mediaList.indexOf(media) % 2 == 0) R.drawable.fallback_coast else R.drawable.fallback_forest) { viewModel.playPhoneMedia(media) } }
}

@Composable
private fun SendPage(state: PhoneUiState, viewModel: PhoneViewModel, padding: PaddingValues) {
    var url by remember { mutableStateOf("") }; var fileName by remember { mutableStateOf("") }; var playlistId by remember { mutableStateOf<String?>(null) }; var expanded by remember { mutableStateOf(false) }
    Page(padding) {
        item { ScreenHeading("DOWNLOAD TO TV", "Send a link", "Paste a direct video URL and let the TV handle the download.") }
        item {
            DwCard {
                Field("Video download URL", url, { url = it }, "https://example.com/movie.mkv")
                Field("Custom file name · optional", fileName, { fileName = it }, "Episode.01.mkv")
                Text("Assign to playlist · optional", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 7.dp))
                Box {
                    Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceDark).border(1.dp, Border, RoundedCornerShape(12.dp)).clickable { expanded = true }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.playlists.find { it.id == playlistId }?.name ?: "Independent download", color = Foreground, modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null, tint = Muted)
                    }
                    DropdownMenu(expanded, { expanded = false }, containerColor = Surface) {
                        DropdownMenuItem({ Text("Independent download") }, { playlistId = null; expanded = false })
                        state.playlists.forEach { p -> DropdownMenuItem({ Text(p.name) }, { playlistId = p.id; expanded = false }) }
                    }
                }
                PrimaryButton("Start download on TV", enabled = state.tvConnected && url.isNotBlank()) { viewModel.sendDownload(url, fileName, playlistId); url = ""; fileName = "" }
            }
        }
        item { SectionTitle("TV download queue", "${state.downloads.summary.active} ACTIVE") }
        if (state.downloads.tasks.isEmpty()) item { EmptyCard("No download tasks on the TV yet.", Icons.Default.Download) }
        else item { DwCard { state.downloads.tasks.forEach { DownloadRow(it) } } }
    }
}

@Composable private fun DownloadRow(task: TvDownloadTask) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(Icons.Default.Download); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.fileName, color = Foreground, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${task.status} · ${task.progress}%${if (task.speed.isNotBlank()) " · ${task.speed}" else ""}", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }; Tag(task.status)
    }
}

@Composable
private fun PlaylistsPage(state: PhoneUiState, viewModel: PhoneViewModel, padding: PaddingValues) {
    var name by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<TvPlaylist?>(null) }
    Page(padding) {
        item { ScreenHeading("ORDERED PLAYBACK", "Playlists", "Group episodes or films and keep their order synced with the TV.") }
        item { DwCard { Field("New playlist or series", name, { name = it }, "Breaking Bad · Season 1"); PrimaryButton("Create playlist", state.tvConnected && name.isNotBlank()) { viewModel.createPlaylist(name); name = "" } } }
        item { SectionTitle("Your playlists", "${state.playlists.size} LIST${if (state.playlists.size == 1) "" else "S"}") }
        if (state.playlists.isEmpty()) item { EmptyCard("No playlists on this TV yet.", Icons.Default.PlaylistPlay) }
        else item {
            DwCard {
                state.playlists.forEachIndexed { index, playlist ->
                    if (index > 0) HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Default.PlaylistAdd); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(playlist.name, color = Foreground, fontWeight = FontWeight.Bold); Text("${playlist.itemCount} ITEMS · SYNCED TO TV", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }
                        IconButton(onClick = { viewModel.playPlaylist(playlist.id) }, enabled = playlist.itemCount > 0) { Icon(Icons.Default.PlayArrow, "Play", tint = if (playlist.itemCount > 0) Foreground else Muted.copy(alpha = .4f)) }
                        IconButton(onClick = { pendingDelete = playlist }) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Muted) }
                    }
                }
            }
        }
    }
    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Surface,
            shape = RoundedCornerShape(19.dp),
            tonalElevation = 0.dp,
            title = {
                Text("Delete playlist?", color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            },
            text = {
                Text("“${playlist.name}” and its playlist order will be removed from the TV. Downloaded files will stay on the device.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = Foreground, fontWeight = FontWeight.Bold) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        pendingDelete = null
                    },
                    shape = RoundedCornerShape(ControlRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Bg)
                ) {
                    Text("Delete playlist", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun NetworkPage(state: PhoneUiState, viewModel: PhoneViewModel, padding: PaddingValues) {
    var tvUrl by remember(state.tvUrl) { mutableStateOf(state.tvUrl) }
    var smbTab by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var server by remember { mutableStateOf("") }; var share by remember { mutableStateOf("") }; var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var port by remember { mutableStateOf("445") }
    Page(padding) {
        item { ScreenHeading("LOCAL & REMOTE", "Network", "Connect phones, WebDAV servers, Windows, Mac, Linux, or NAS folders.") }
        item {
            DwCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Label("TV CONNECTION"); Text(if (state.tvConnected) "Living Room TV" else "Connect to dwPlayer", color = Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif) }; Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.tvConnected) Accent else Muted)) }
                Field("TV address", tvUrl, { tvUrl = it }, "http://192.168.1.24:8200")
                PrimaryButton(if (state.tvConnected) "Reconnect" else "Connect", true) { viewModel.connectTv(tvUrl) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 112.dp).clip(RoundedCornerShape(19.dp)).background(Surface).border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(19.dp)).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Local Wi-Fi discovery",
                        color = Foreground,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("Find compatible devices on\nthis network.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                SmallAccentButton(if (state.isScanningNetwork) "Scanning…" else "Scan") { viewModel.scanNetwork() }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Surface).border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(13.dp)).padding(4.dp)) {
                Segment("WebDAV / phone", !smbTab, Modifier.weight(1f)) { smbTab = false }
                Segment("SMB share", smbTab, Modifier.weight(1f)) { smbTab = true }
            }
        }
        item {
            DwCard {
                Text(if (smbTab) "Add SMB share" else "Add WebDAV", color = Foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(if (smbTab) "For Windows, Mac, Linux, and NAS folders." else "For Android phones, Nextcloud, or HTTP-range servers.", color = Muted, fontSize = 11.sp)
                Field(if (smbTab) "Display name" else "Server name", name, { name = it }, if (smbTab) "Home NAS" else "My phone or Nextcloud")
                Field(if (smbTab) "Host or IP" else "Server URL", server, { server = it }, if (smbTab) "192.168.1.50 or mynas.local" else "http://192.168.1.50:8080/webdav")
                if (smbTab) Field("Share name", share, { share = it }, "Movies")
                Field(if (smbTab) "Username · guest allowed" else "Username · optional", username, { username = it }, "user")
                Field("Password · optional", password, { password = it }, "••••••••", password = true)
                if (smbTab) Field("Port", port, { port = it }, "445")
                PrimaryButton(if (smbTab) "Save SMB share to TV" else "Save WebDAV to TV", state.tvConnected) {
                    if (smbTab) viewModel.saveSmb(name, server, share, username, password, port) else viewModel.saveWebDav(name, server, username, password)
                    name = ""; server = ""; share = ""; username = ""; password = ""
                }
            }
        }
        val sourceCount = state.webDavServers.size + state.smbShares.size
        item { SectionTitle("Configured sources", "$sourceCount SAVED") }
        if (sourceCount == 0) item { EmptyCard("No network sources saved on this TV.", Icons.Default.Cloud) }
        else item {
            DwCard {
                state.webDavServers.forEach { SourceRow(it.name, "WEBDAV · SYNCED TO TV") { viewModel.deleteWebDav(it.id) } }
                state.smbShares.forEach { SourceRow(it.name, "SMB · ${it.host}/${it.shareName}") { viewModel.deleteSmb(it.id) } }
            }
        }
        if (state.discoveredServers.isNotEmpty()) {
            item { SectionTitle("Discovered devices", "${state.discoveredServers.size} FOUND") }
            item { DwCard { state.discoveredServers.forEach { SourceRow(it.serviceName, "${it.deviceType.uppercase()} · ${it.url}", null) } } }
        }
    }
}

@Composable private fun SourceRow(name: String, meta: String, delete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RowIcon(Icons.Default.Cloud); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(name, color = Foreground, fontWeight = FontWeight.Bold); Text(meta, color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis) }; delete?.let { IconButton(onClick = it) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Muted) } } }
}

@Composable private fun Segment(text: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) Foreground else Color.Transparent).clickable(onClick = action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Bg else Muted,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, password: Boolean = false) {
    Text(label, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 7.dp))
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = { Text(placeholder, color = Muted.copy(alpha = .6f)) }, singleLine = true, visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Border, focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark, cursorColor = Accent, focusedTextColor = Foreground, unfocusedTextColor = Foreground))
}

@Composable private fun PrimaryButton(text: String, enabled: Boolean = true, action: () -> Unit) { Button(action, Modifier.fillMaxWidth().padding(top = 17.dp).height(48.dp), enabled = enabled, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg, disabledContainerColor = Border, disabledContentColor = Muted)) { Text(text, fontWeight = FontWeight.Bold) } }
@Composable private fun SmallButton(text: String, action: () -> Unit) {
    Box(
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(Surface)
            .border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(ControlRadius))
            .clickable(onClick = action)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
@Composable private fun SmallAccentButton(text: String, action: () -> Unit) {
    Button(
        action,
        modifier = Modifier.width(112.dp).height(54.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(title: String, actionText: String? = null, action: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Foreground, fontSize = 21.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        if (actionText != null) {
            if (action != null) {
                Text(actionText, color = Muted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = action).padding(8.dp))
            } else {
                Tag(actionText)
            }
        }
    }
}
@Composable private fun Label(text: String) { Text(text, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = .7.sp) }
@Composable private fun Tag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(ControlBorderWidth, ControlBorderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Muted, fontSize = 9.sp, lineHeight = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier, dark: Boolean = false) { Column(modifier) { Text(label, color = if (dark) Bg.copy(alpha = .62f) else Muted, fontSize = 10.sp); Text(value, color = if (dark) Bg else Foreground, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1) } }
@Composable private fun RowIcon(icon: ImageVector) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Foreground.copy(alpha = .07f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Muted) } }

@Composable private fun MediaRow(title: String, meta: String, image: Int, action: (() -> Unit)? = null) {
    val interaction = if (action != null) Modifier.clickable(onClick = action) else Modifier
    Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(16.dp)).then(interaction).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), null, Modifier.width(96.dp).height(64.dp).clip(RoundedCornerShape(11.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = Foreground, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(meta, color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1) }; Text("›", color = Muted, fontSize = 20.sp)
    }
}

@Composable private fun EmptyCard(message: String, icon: ImageVector) { DwCard { Column(Modifier.fillMaxWidth().heightIn(min = 104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = Muted); Spacer(Modifier.height(8.dp)); Text(message, color = Muted, fontSize = 11.sp) } } }

private fun formatSize(bytes: Long): String { val mb = bytes / 1048576.0; return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.1f MB", mb) }
