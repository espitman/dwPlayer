@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dwplayer.core.downloader.DownloadService
import com.dwplayer.core.webserver.KtorService
import com.dwplayer.ui.components.NavDestination
import com.dwplayer.ui.components.TopStatusBar
import com.dwplayer.ui.components.TvSidebar
import com.dwplayer.ui.player.PlayerActivity
import com.dwplayer.ui.screens.*
import com.dwplayer.ui.theme.DwPlayerTheme
import com.dwplayer.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screensaver / sleep while app is running in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive TV Fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Start Ktor and Download background services
        startBackgroundServices()
        requestStoragePermissions()

        setContent {
            var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
            var showAddDialog by remember { mutableStateOf(false) }
            var addDialogInitialPlaylistId by remember { mutableStateOf<String?>(null) }

            val tasks by viewModel.tasks.collectAsState()
            val playlists by viewModel.playlists.collectAsState()
            val liveProgress by viewModel.liveProgress.collectAsState()
            val smbShares by viewModel.smbShares.collectAsState()
            val historyList by viewModel.playbackHistory.collectAsState()
            val storageInfo by viewModel.storageInfo.collectAsState()
            val companionUrl by viewModel.companionUrl.collectAsState()
            val archiveFiles by viewModel.archiveFiles.collectAsState()

            val currentSmbShare by viewModel.currentSmbShare.collectAsState()
            val currentSmbPath by viewModel.currentSmbPath.collectAsState()
            val smbItems by viewModel.smbItems.collectAsState()
            val isSmbLoading by viewModel.isSmbLoading.collectAsState()

            val webDavServers by viewModel.webDavServers.collectAsState()
            val currentWebDavServer by viewModel.currentWebDavServer.collectAsState()
            val currentWebDavPath by viewModel.currentWebDavPath.collectAsState()
            val webDavItems by viewModel.webDavItems.collectAsState()
            val isWebDavLoading by viewModel.isWebDavLoading.collectAsState()
            val webDavError by viewModel.webDavError.collectAsState()
            val discoveredServers by viewModel.discoveredServers.collectAsState()

            val sidebarFocusRequester = remember { FocusRequester() }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                DwPlayerTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF090C0A))
                    ) {
                        // Main Navigation Layout (Left Icon Rail + Content)
                        Row(modifier = Modifier.fillMaxSize()) {
                            TvSidebar(
                                currentDestination = currentDestination,
                                onNavigate = { currentDestination = it },
                                onAddClicked = { showAddDialog = true },
                                firstItemFocusRequester = sidebarFocusRequester
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Global Top Status Bar
                                TopStatusBar(companionUrl = companionUrl)

                                // Active Destination Screen
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    when (currentDestination) {
                                        NavDestination.HOME -> {
                                            HomeScreen(
                                                historyList = historyList,
                                                activeTasks = tasks.filter { it.status == "ACTIVE" || it.status == "PENDING" },
                                                liveProgress = liveProgress,
                                                smbShares = smbShares,
                                                storageInfo = storageInfo,
                                                companionUrl = companionUrl,
                                                onPlayMedia = { uri, title, isSmb ->
                                                    playMedia(uri, title, isSmb)
                                                },
                                                onNavigateDownloads = { currentDestination = NavDestination.DOWNLOADS },
                                                onNavigateSmb = { currentDestination = NavDestination.SMB },
                                                onNavigateArchive = { currentDestination = NavDestination.ARCHIVE },
                                                onOpenAddDialog = { showAddDialog = true },
                                                onClearHistory = { viewModel.clearPlaybackHistory() }
                                            )
                                        }
                                        NavDestination.DOWNLOADS -> {
                                            DownloadsScreen(
                                                tasks = tasks,
                                                liveProgress = liveProgress,
                                                playlists = playlists,
                                                onPlayTask = { task ->
                                                    val file = File(task.targetFolder, task.fileName)
                                                    playMedia(file.absolutePath, task.fileName, isSmb = false)
                                                },
                                                onPauseTask = { viewModel.pauseDownload(it) },
                                                onResumeTask = { viewModel.resumeDownload(it) },
                                                onDeleteTask = { id, delFile -> viewModel.deleteDownload(id, delFile) },
                                                onPauseAll = { viewModel.pauseAll() },
                                                onResumeAll = { viewModel.resumeAll() },
                                                onOpenAddDialog = { showAddDialog = true },
                                                onAddToPlaylist = { playlistId, title, uri ->
                                                    viewModel.addPlaylistItem(playlistId, title, uri)
                                                    android.widget.Toast.makeText(this@MainActivity, "Added to Series: $title", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onCreatePlaylist = { name ->
                                                    viewModel.createPlaylist(name)
                                                }
                                            )
                                        }
                                        NavDestination.PLAYLISTS -> {
                                            PlaylistsScreen(
                                                playlists = playlists,
                                                archiveFiles = archiveFiles,
                                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                                onRemoveItemFromPlaylist = { viewModel.deletePlaylistItem(it) },
                                                onAddItemsToPlaylist = { playlistId, files ->
                                                    files.forEach { file ->
                                                        viewModel.addPlaylistItem(playlistId, file.name, file.path)
                                                    }
                                                    android.widget.Toast.makeText(this@MainActivity, "Added ${files.size} videos to Series", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onPlayItem = { item, _ ->
                                                    playPlaylistItem(item)
                                                },
                                                onPlayAll = { series ->
                                                    if (series.items.isNotEmpty()) {
                                                        playPlaylistItem(series.items.first())
                                                    }
                                                }
                                            )
                                        }
                                        NavDestination.ARCHIVE -> {
                                            LaunchedEffect(Unit) {
                                                viewModel.refreshArchiveFiles()
                                            }
                                            MediaArchiveScreen(
                                                files = archiveFiles,
                                                storageInfo = storageInfo,
                                                playlists = playlists,
                                                onPlayFile = { file ->
                                                    playMedia(file.path, file.name, isSmb = false)
                                                },
                                                onDeleteFile = { file ->
                                                    viewModel.deleteArchiveFile(file)
                                                },
                                                onRefresh = {
                                                    viewModel.refreshArchiveFiles()
                                                },
                                                onAddToPlaylist = { playlistId, title, uri ->
                                                    viewModel.addPlaylistItem(playlistId, title, uri)
                                                    android.widget.Toast.makeText(this@MainActivity, "Added to Series: $title", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onCreatePlaylist = { name ->
                                                    viewModel.createPlaylist(name)
                                                }
                                            )
                                        }
                                        NavDestination.SMB -> {
                                            NetworkSharesScreen(
                                                smbShares = smbShares,
                                                currentSmbShare = currentSmbShare,
                                                currentSmbPath = currentSmbPath,
                                                smbItems = smbItems,
                                                isSmbLoading = isSmbLoading,
                                                onSelectSmbShare = { viewModel.selectSmbShare(it) },
                                                onNavigateSmbPath = { viewModel.browseSmbPath(it) },
                                                onBackSmbPath = { viewModel.navigateSmbUp() },
                                                onPlaySmbFile = { share, path, title ->
                                                    playSmbMedia(share.id, path, title)
                                                },
                                                onDownloadSmbFile = { share, item ->
                                                    viewModel.downloadSmbFile(share, item)
                                                },
                                                onAddSmbShare = { name, host, shareName, user, pass, domain ->
                                                    viewModel.addSmbShare(name, host, shareName, user, pass, domain)
                                                },
                                                onDeleteSmbShare = { viewModel.deleteSmbShare(it) },

                                                webDavServers = webDavServers,
                                                currentWebDavServer = currentWebDavServer,
                                                currentWebDavPath = currentWebDavPath,
                                                webDavItems = webDavItems,
                                                isWebDavLoading = isWebDavLoading,
                                                webDavError = webDavError,
                                                onSelectWebDavServer = { viewModel.selectWebDavServer(it) },
                                                onNavigateWebDavPath = { viewModel.browseWebDavPath(it) },
                                                onBackWebDavPath = { viewModel.navigateWebDavUp() },
                                                onPlayWebDavFile = { server, item ->
                                                    playWebDavMedia(server, item)
                                                },
                                                onDownloadWebDavFile = { server, item ->
                                                    viewModel.enqueueDownload(item.fullUrl, item.name)
                                                    android.widget.Toast.makeText(this@MainActivity, "Added to Downloads: ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onAddWebDavServer = { name, url, user, pass, cb ->
                                                    viewModel.addWebDavServer(name, url, user, pass, cb)
                                                },
                                                onDeleteWebDavServer = { viewModel.deleteWebDavServer(it) },

                                                discoveredServers = discoveredServers,
                                                onAddDiscoveredServer = { viewModel.addDiscoveredServer(it) }
                                            )
                                        }
                                        NavDestination.SETTINGS -> {
                                            SettingsScreen(
                                                companionUrl = companionUrl,
                                                storageInfo = storageInfo
                                            )
                                        }
                                        NavDestination.ADD -> {}
                                    }
                                }
                            }
                        }

                        // Add Download Link Modal
                        if (showAddDialog) {
                            AddDownloadDialog(
                                companionUrl = companionUrl,
                                playlists = playlists,
                                initialPlaylistId = addDialogInitialPlaylistId,
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onDismiss = {
                                    showAddDialog = false
                                    addDialogInitialPlaylistId = null
                                },
                                onAddUrl = { url, name, playlistId ->
                                    viewModel.enqueueDownload(url, name, playlistId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playPlaylistItem(item: com.dwplayer.data.entities.PlaylistItemEntity) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            var finalUri = item.mediaUri
            if (!item.downloadTaskId.isNullOrBlank()) {
                val task = viewModel.tasks.value.find { it.id == item.downloadTaskId }
                if (task != null) {
                    finalUri = File(task.targetFolder, task.fileName).absolutePath
                }
            }
            putExtra("MEDIA_URI", finalUri)
            putExtra("MEDIA_TITLE", item.title)
            putExtra("PLAYLIST_ID", item.playlistId)
            putExtra("PLAYLIST_ITEM_ID", item.id)
        }
        startActivity(intent)
    }

    private fun playMedia(mediaUri: String, title: String, isSmb: Boolean) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("MEDIA_URI", mediaUri)
            putExtra("MEDIA_TITLE", title)
            putExtra("IS_SMB", isSmb)
        }
        startActivity(intent)
    }

    private fun playSmbMedia(shareId: String, filePath: String, title: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("MEDIA_URI", "smb://$shareId/$filePath")
            putExtra("SMB_SHARE_ID", shareId)
            putExtra("SMB_FILE_PATH", filePath)
            putExtra("MEDIA_TITLE", title)
            putExtra("IS_SMB", true)
        }
        startActivity(intent)
    }

    private fun playWebDavMedia(server: com.dwplayer.data.entities.WebDavServerEntity, item: com.dwplayer.data.models.WebDavItem) {
        val authHeader = if (!server.username.isNullOrBlank()) {
            val credentials = "${server.username}:${server.password ?: ""}"
            val encoded = android.util.Base64.encodeToString(credentials.toByteArray(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
            "Basic $encoded"
        } else null

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("MEDIA_URI", item.fullUrl)
            putExtra("MEDIA_TITLE", item.name)
            if (authHeader != null) {
                putExtra("AUTH_HEADER", authHeader)
            }
        }
        startActivity(intent)
    }

    private fun startBackgroundServices() {
        // Start Ktor Service
        val ktorIntent = Intent(this, KtorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(ktorIntent)
        } else {
            startService(ktorIntent)
        }

        // Start Download Service
        val downloadIntent = Intent(this, DownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(downloadIntent)
        } else {
            startService(downloadIntent)
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore if not supported on this TV
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
