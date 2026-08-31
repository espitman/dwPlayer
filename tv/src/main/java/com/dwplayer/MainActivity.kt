@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.dwplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
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
import com.dwplayer.ui.theme.BgDark
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

        val installedVersion = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "Unknown"

        setContent {
            var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
            val navigationHistory = remember { mutableStateListOf(NavDestination.HOME) }
            var showAddDialog by remember { mutableStateOf(false) }
            var lastHomeBackPressAt by remember { mutableLongStateOf(0L) }

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
            val smbError by viewModel.smbError.collectAsState()

            val webDavServers by viewModel.webDavServers.collectAsState()
            val currentWebDavServer by viewModel.currentWebDavServer.collectAsState()
            val currentWebDavPath by viewModel.currentWebDavPath.collectAsState()
            val webDavItems by viewModel.webDavItems.collectAsState()
            val isWebDavLoading by viewModel.isWebDavLoading.collectAsState()
            val webDavError by viewModel.webDavError.collectAsState()
            val discoveredServers by viewModel.discoveredServers.collectAsState()
            val subtitleSettings by viewModel.subtitleSettings.collectAsState()

            val sidebarFocusRequester = remember { FocusRequester() }

            fun navigateTo(destination: NavDestination) {
                if (destination == NavDestination.ADD || destination == currentDestination) return

                lastHomeBackPressAt = 0L
                if (destination == NavDestination.HOME) {
                    navigationHistory.clear()
                    navigationHistory.add(NavDestination.HOME)
                } else {
                    navigationHistory.add(destination)
                }
                currentDestination = destination
            }

            BackHandler {
                when {
                    showAddDialog -> {
                        showAddDialog = false
                        lastHomeBackPressAt = 0L
                    }

                    currentDestination == NavDestination.SMB && currentSmbShare != null -> {
                        viewModel.navigateSmbUp()
                        lastHomeBackPressAt = 0L
                    }

                    currentDestination == NavDestination.SMB && currentWebDavServer != null -> {
                        viewModel.navigateWebDavUp()
                        lastHomeBackPressAt = 0L
                    }

                    navigationHistory.size > 1 -> {
                        navigationHistory.removeAt(navigationHistory.lastIndex)
                        currentDestination = navigationHistory.last()
                        lastHomeBackPressAt = 0L
                    }

                    currentDestination != NavDestination.HOME -> {
                        navigationHistory.clear()
                        navigationHistory.add(NavDestination.HOME)
                        currentDestination = NavDestination.HOME
                        lastHomeBackPressAt = 0L
                    }

                    else -> {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastHomeBackPressAt <= HOME_EXIT_CONFIRMATION_WINDOW_MS) {
                            finish()
                        } else {
                            lastHomeBackPressAt = now
                            Toast.makeText(
                                this@MainActivity,
                                "Press Back again to exit",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                DwPlayerTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgDark)
                    ) {
                        // Main Navigation Layout (Left Icon Rail + Content)
                        Row(modifier = Modifier.fillMaxSize()) {
                            TvSidebar(
                                currentDestination = currentDestination,
                                onNavigate = { navigateTo(it) },
                                onAddClicked = {
                                    lastHomeBackPressAt = 0L
                                    showAddDialog = true
                                },
                                firstItemFocusRequester = sidebarFocusRequester
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Global Top Status Bar
                                TopStatusBar(
                                    companionUrl = companionUrl,
                                    modifier = Modifier.zIndex(2f)
                                )

                                // Active Destination Screen
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .zIndex(1f)
                                ) {
                                    when (currentDestination) {
                                        NavDestination.HOME -> {
                                            HomeScreen(
                                                historyList = historyList,
                                                activeTasks = tasks.filter { it.status == "ACTIVE" || it.status == "PENDING" },
                                                liveProgress = liveProgress,
                                                smbShares = smbShares,
                                                webDavServers = webDavServers,
                                                archiveFiles = archiveFiles,
                                                storageInfo = storageInfo,
                                                companionUrl = companionUrl,
                                                onPlayMedia = { uri, title, isSmb ->
                                                    playMedia(uri, title, isSmb)
                                                },
                                                onNavigateDownloads = { navigateTo(NavDestination.DOWNLOADS) },
                                                onNavigateSmb = { navigateTo(NavDestination.SMB) },
                                                onNavigateArchive = { navigateTo(NavDestination.ARCHIVE) },
                                                onOpenAddDialog = {
                                                    lastHomeBackPressAt = 0L
                                                    showAddDialog = true
                                                },
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
                                                onOpenAddDialog = {
                                                    lastHomeBackPressAt = 0L
                                                    showAddDialog = true
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
                                                playbackHistory = historyList,
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
                                                smbError = smbError,
                                                onSelectSmbShare = { viewModel.selectSmbShare(it) },
                                                onNavigateSmbPath = { viewModel.browseSmbPath(it) },
                                                onBackSmbPath = { viewModel.navigateSmbUp() },
                                                onPlaySmbFile = { share, path, title ->
                                                    playSmbMedia(share.id, path, title)
                                                },
                                                onDownloadSmbFile = { share, item ->
                                                    viewModel.downloadSmbFile(share, item)
                                                },
                                                onAddSmbShare = { name, host, shareName, user, pass, domain, cb ->
                                                    viewModel.addSmbShare(name, host, shareName, user, pass, domain, cb)
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
                                                storageInfo = storageInfo,
                                                subtitleSettings = subtitleSettings,
                                                appVersion = installedVersion,
                                                deviceName = Build.MODEL,
                                                androidVersion = Build.VERSION.RELEASE
                                            )
                                        }
                                        NavDestination.ADD -> {}
                                    }
                                }
                            }
                        }

                        // Add Download Link Modal
                        if (showAddDialog) {
                            AddUrlDrawer(
                                onDismiss = {
                                    showAddDialog = false
                                },
                                onOpenUrl = { url ->
                                    val streamTitle = Uri.parse(url).lastPathSegment
                                        ?.takeIf { it.isNotBlank() }
                                        ?: "Network stream"
                                    playMedia(url, streamTitle, isSmb = false)
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

    private companion object {
        const val HOME_EXIT_CONFIRMATION_WINDOW_MS = 2_000L
    }
}
