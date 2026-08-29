package com.dwplayer.core.downloader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.models.DownloadProgressInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DwDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadTaskDao: DownloadTaskDao,
    private val storageManager: StorageManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "DwDownloadManager"
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloaders = ConcurrentHashMap<String, MultiSegmentDownloader>()

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadProgressInfo>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadProgressInfo>> = _downloadStatus.asStateFlow()

    fun enqueueDownload(url: String, customFileName: String? = null, customId: String? = null): String {
        val taskId = customId ?: UUID.randomUUID().toString()
        val targetFolder = storageManager.getDownloadDirectory()
        val defaultName = customFileName ?: "video_${System.currentTimeMillis()}.mp4"

        managerScope.launch {
            val task = DownloadTaskEntity(
                id = taskId,
                url = url,
                targetFolder = targetFolder.absolutePath,
                fileName = defaultName,
                status = "PENDING"
            )
            downloadTaskDao.insertTask(task)
            startDownloadService(taskId)
        }
        return taskId
    }

    fun startDownload(taskId: String) {
        managerScope.launch {
            val task = downloadTaskDao.getTaskById(taskId) ?: return@launch
            if (activeDownloaders.containsKey(taskId)) return@launch

            downloadTaskDao.updateStatus(taskId, "ACTIVE")

            val downloader = MultiSegmentDownloader(
                taskId = taskId,
                url = task.url,
                targetFolder = File(task.targetFolder),
                customFileName = task.fileName,
                okHttpClient = okHttpClient,
                onProgress = { info ->
                    _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                        put(taskId, info)
                    }
                    managerScope.launch {
                        downloadTaskDao.updateProgress(
                            id = taskId,
                            progress = info.progress,
                            downloaded = info.downloadedBytes,
                            total = info.totalBytes
                        )
                    }
                },
                onComplete = { file ->
                    activeDownloaders.remove(taskId)
                    _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                        remove(taskId)
                    }
                    managerScope.launch {
                        downloadTaskDao.markCompleted(taskId)
                    }
                    Log.i(TAG, "Task $taskId completed: ${file.absolutePath}")
                },
                onError = { err ->
                    activeDownloaders.remove(taskId)
                    _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
                        remove(taskId)
                    }
                    managerScope.launch {
                        downloadTaskDao.markFailed(taskId, err)
                    }
                    Log.e(TAG, "Task $taskId failed: $err")
                }
            )

            activeDownloaders[taskId] = downloader
            downloader.start(managerScope)
        }
    }

    fun pauseDownload(taskId: String) {
        val downloader = activeDownloaders.remove(taskId)
        downloader?.pause()
        _downloadStatus.value = _downloadStatus.value.toMutableMap().apply {
            remove(taskId)
        }
        managerScope.launch {
            downloadTaskDao.updateStatus(taskId, "PAUSED")
        }
    }

    fun resumeDownload(taskId: String) {
        startDownloadService(taskId)
    }

    fun deleteDownload(taskId: String, deleteFile: Boolean = true) {
        pauseDownload(taskId)
        managerScope.launch {
            val task = downloadTaskDao.getTaskById(taskId)
            if (task != null) {
                if (deleteFile) {
                    val finalFile = File(task.targetFolder, task.fileName)
                    val partFile = File(task.targetFolder, "${task.fileName}.dwpart")
                    if (finalFile.exists()) finalFile.delete()
                    if (partFile.exists()) partFile.delete()
                }
                downloadTaskDao.deleteTaskById(taskId)
            }
        }
    }

    fun pauseAll() {
        activeDownloaders.keys.forEach { pauseDownload(it) }
    }

    fun resumeAll() {
        managerScope.launch {
            // Service handles resume all
        }
    }

    private fun startDownloadService(taskId: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("TASK_ID", taskId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
