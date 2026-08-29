package com.dwplayer.phone.core.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class MediaItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val size: Long,
    val durationMs: Long,
    val mimeType: String,
    val dateModified: Long
)

@Singleton
class PhoneMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderPreferences: FolderPreferences
) {
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "flv", "wmv", "3gp")

    suspend fun getVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val treeUri = folderPreferences.getFolderUri() ?: return@withContext emptyList()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()

        val videos = mutableListOf<MediaItem>()
        scanFolderRecursively(rootDoc, videos)
        videos.sortedBy { it.title.lowercase() }
    }

    private fun scanFolderRecursively(dir: DocumentFile, outList: MutableList<MediaItem>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanFolderRecursively(file, outList)
            } else if (file.isFile) {
                val name = file.name ?: ""
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = file.type ?: ""
                val isVideo = videoExtensions.contains(ext) || mime.startsWith("video/")

                if (isVideo && file.length() > 0) {
                    val id = (file.uri.toString().hashCode().toLong() and 0x7FFFFFFF)
                    outList.add(
                        MediaItem(
                            id = id,
                            title = name,
                            uri = file.uri,
                            size = file.length(),
                            durationMs = 0L,
                            mimeType = if (mime.isNotBlank()) mime else "video/mp4",
                            dateModified = file.lastModified()
                        )
                    )
                }
            }
        }
    }

    fun findMediaById(id: Long): MediaItem? {
        val treeUri = folderPreferences.getFolderUri() ?: return null
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val videos = mutableListOf<MediaItem>()
        scanFolderRecursively(rootDoc, videos)
        return videos.find { it.id == id }
    }
}
