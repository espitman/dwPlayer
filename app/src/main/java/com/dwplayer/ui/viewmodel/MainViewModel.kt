package com.dwplayer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwplayer.core.downloader.DwDownloadManager
import com.dwplayer.core.downloader.StorageManager
import com.dwplayer.core.smb.SmbClientManager
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.daos.PlaybackHistoryDao
import com.dwplayer.data.daos.SmbShareDao
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.models.DownloadProgressInfo
import com.dwplayer.data.models.LocalArchiveFile
import com.dwplayer.data.models.SmbItem
import com.dwplayer.data.models.StorageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadTaskDao: DownloadTaskDao,
    private val smbShareDao: SmbShareDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val playlistDao: com.dwplayer.data.daos.PlaylistDao,
    val downloadManager: DwDownloadManager,
    private val storageManager: StorageManager,
    private val smbClientManager: SmbClientManager
) : ViewModel() {

    val tasks: StateFlow<List<DownloadTaskEntity>> = downloadTaskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<com.dwplayer.data.entities.PlaylistWithItems>> = playlistDao.getAllPlaylistsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveProgress: StateFlow<Map<String, DownloadProgressInfo>> = downloadManager.downloadStatus

    val smbShares: StateFlow<List<SmbShareEntity>> = smbShareDao.getAllShares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackHistory: StateFlow<List<PlaybackHistoryEntity>> = playbackHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageInfo = MutableStateFlow(storageManager.getStorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _archiveFiles = MutableStateFlow<List<LocalArchiveFile>>(emptyList())
    val archiveFiles: StateFlow<List<LocalArchiveFile>> = _archiveFiles.asStateFlow()

    private val _companionUrl = MutableStateFlow("http://127.0.0.1:8200")
    val companionUrl: StateFlow<String> = _companionUrl.asStateFlow()

    // SMB Explorer State
    private val _currentSmbShare = MutableStateFlow<SmbShareEntity?>(null)
    val currentSmbShare: StateFlow<SmbShareEntity?> = _currentSmbShare.asStateFlow()

    private val _currentSmbPath = MutableStateFlow("")
    val currentSmbPath: StateFlow<String> = _currentSmbPath.asStateFlow()

    private val _smbItems = MutableStateFlow<List<SmbItem>>(emptyList())
    val smbItems: StateFlow<List<SmbItem>> = _smbItems.asStateFlow()

    private val _isSmbLoading = MutableStateFlow(false)
    val isSmbLoading: StateFlow<Boolean> = _isSmbLoading.asStateFlow()

    init {
        detectLocalIp()
        refreshStorage()
        refreshArchiveFiles()
    }

    fun refreshStorage() {
        _storageInfo.value = storageManager.getStorageInfo()
    }

    fun refreshArchiveFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _archiveFiles.value = storageManager.getLocalArchiveFiles()
            refreshStorage()
        }
    }

    fun deleteArchiveFile(file: LocalArchiveFile) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.deleteLocalArchiveFile(file.path)
            try {
                val matching = downloadTaskDao.getAllTasksList().find { it.fileName == file.name }
                matching?.let { downloadTaskDao.deleteTaskById(it.id) }
            } catch (ignored: Exception) {}
            refreshArchiveFiles()
        }
    }

    private fun detectLocalIp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (intf in interfaces) {
                    val addrs = intf.inetAddresses
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: "127.0.0.1"
                            _companionUrl.value = "http://$ip:8200"
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                _companionUrl.value = "http://127.0.0.1:8200"
            }
        }
    }

    fun enqueueDownload(url: String, fileName: String? = null, playlistId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val taskId = downloadManager.enqueueDownload(url, fileName)
            if (!playlistId.isNullOrBlank()) {
                val count = playlistDao.getItemCount(playlistId)
                val task = downloadTaskDao.getTaskById(taskId)
                val title = fileName?.ifBlank { null }
                    ?: url.substringAfterLast("/").substringBefore("?").ifBlank { "Episode ${count + 1}" }
                val targetFile = if (task != null) java.io.File(task.targetFolder, task.fileName).absolutePath else ""
                playlistDao.insertPlaylistItem(
                    com.dwplayer.data.entities.PlaylistItemEntity(
                        playlistId = playlistId,
                        title = title,
                        mediaUri = targetFile,
                        downloadTaskId = taskId,
                        orderIndex = count
                    )
                )
            }
            refreshStorage()
        }
    }

    // Playlist Methods
    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank()) {
                playlistDao.insertPlaylist(com.dwplayer.data.entities.PlaylistEntity(name = name.trim()))
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
        }
    }

    fun addPlaylistItem(playlistId: String, title: String, mediaUri: String, downloadTaskId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = playlistDao.getItemCount(playlistId)
            playlistDao.insertPlaylistItem(
                com.dwplayer.data.entities.PlaylistItemEntity(
                    playlistId = playlistId,
                    title = title.ifBlank { "Episode ${count + 1}" },
                    mediaUri = mediaUri,
                    downloadTaskId = downloadTaskId,
                    orderIndex = count
                )
            )
        }
    }

    fun deletePlaylistItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylistItem(itemId)
        }
    }

    fun reorderPlaylistItems(playlistId: String, orderedItemIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.reorderItems(orderedItemIds)
        }
    }

    fun pauseDownload(taskId: String) {
        downloadManager.pauseDownload(taskId)
    }

    fun resumeDownload(taskId: String) {
        downloadManager.resumeDownload(taskId)
    }

    fun deleteDownload(taskId: String, deleteFile: Boolean = true) {
        downloadManager.deleteDownload(taskId, deleteFile)
        refreshStorage()
    }

    fun pauseAll() {
        downloadManager.pauseAll()
    }

    fun resumeAll() {
        downloadManager.resumeAll()
    }

    // SMB Methods
    fun addSmbShare(name: String, host: String, shareName: String, user: String?, pass: String?, domain: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = SmbShareEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                host = host,
                shareName = shareName,
                username = user,
                password = pass,
                domain = domain
            )
            smbShareDao.insertShare(entity)
        }
    }

    fun deleteSmbShare(shareId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            smbShareDao.deleteShareById(shareId)
            if (_currentSmbShare.value?.id == shareId) {
                _currentSmbShare.value = null
                _smbItems.value = emptyList()
            }
        }
    }

    fun selectSmbShare(share: SmbShareEntity) {
        _currentSmbShare.value = share
        _currentSmbPath.value = ""
        browseSmbFolder(share, "")
    }

    fun browseSmbPath(path: String) {
        val share = _currentSmbShare.value ?: return
        _currentSmbPath.value = path
        browseSmbFolder(share, path)
    }

    fun navigateSmbUp() {
        val path = _currentSmbPath.value
        if (path.isBlank()) {
            _currentSmbShare.value = null
            _smbItems.value = emptyList()
        } else {
            val parent = path.substringBeforeLast("/", "")
            browseSmbPath(parent)
        }
    }

    private fun browseSmbFolder(share: SmbShareEntity, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSmbLoading.value = true
            val items = smbClientManager.listDirectory(share, path)
            _smbItems.value = items
            _isSmbLoading.value = false
        }
    }

    fun downloadSmbFile(share: SmbShareEntity, item: SmbItem) {
        // Enqueue stream URL
        // SMB file direct download to TV
        enqueueDownload("smb://${share.id}/${item.path}", item.name)
    }
}
