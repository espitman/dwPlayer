package com.dwplayer.phone.core.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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

data class PhoneDirectoryItem(
    val name: String,
    val isDirectory: Boolean,
    val relativePath: String,
    val mediaItem: MediaItem? = null
)

@Singleton
class PhoneMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderPreferences: FolderPreferences
) {
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "flv", "wmv", "3gp")
    private val mediaCache = ConcurrentHashMap<Long, MediaItem>()
    @Volatile private var cachedList: List<MediaItem> = emptyList()

    suspend fun getVideos(forceRefresh: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedList.isNotEmpty()) {
            return@withContext cachedList
        }

        val treeUri = folderPreferences.getFolderUri() ?: return@withContext emptyList()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()

        val videos = mutableListOf<MediaItem>()
        val newMap = ConcurrentHashMap<Long, MediaItem>()
        scanFolderRecursively(rootDoc, videos, newMap)
        val sorted = videos.sortedBy { it.title.lowercase() }
        
        mediaCache.clear()
        mediaCache.putAll(newMap)
        cachedList = sorted
        sorted
    }

    suspend fun listFolderContent(relativePath: String = ""): List<PhoneDirectoryItem> = withContext(Dispatchers.IO) {
        val treeUri = folderPreferences.getFolderUri() ?: return@withContext emptyList()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()

        val targetDir = resolveDirectory(rootDoc, relativePath) ?: return@withContext emptyList()
        val result = mutableListOf<PhoneDirectoryItem>()
        val files = targetDir.listFiles()

        for (file in files) {
            val name = file.name ?: ""
            // Filter out hidden files and folders (starting with .)
            if (name.isBlank() || name.startsWith(".")) continue

            val cleanRelative = if (relativePath.isBlank()) name else "$relativePath/$name"

            if (file.isDirectory) {
                result.add(
                    PhoneDirectoryItem(
                        name = name,
                        isDirectory = true,
                        relativePath = cleanRelative
                    )
                )
            } else if (file.isFile) {
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = file.type ?: ""
                val isVideo = videoExtensions.contains(ext) || mime.startsWith("video/")

                if (isVideo && file.length() > 0) {
                    val id = (file.uri.toString().hashCode().toLong() and 0x7FFFFFFF)
                    val item = MediaItem(
                        id = id,
                        title = name,
                        uri = file.uri,
                        size = file.length(),
                        durationMs = 0L,
                        mimeType = getAccurateMimeType(name, mime),
                        dateModified = file.lastModified()
                    )
                    mediaCache[id] = item
                    result.add(
                        PhoneDirectoryItem(
                            name = name,
                            isDirectory = false,
                            relativePath = cleanRelative,
                            mediaItem = item
                        )
                    )
                }
            }
        }

        result.sortedWith(
            compareByDescending<PhoneDirectoryItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun resolveDirectory(root: DocumentFile, relativePath: String): DocumentFile? {
        val clean = relativePath.trim().removePrefix("/").removeSuffix("/")
        if (clean.isBlank()) return root

        val parts = clean.split("/")
        var current: DocumentFile = root
        for (part in parts) {
            if (part.isBlank() || part == ".") continue
            var nextDir: DocumentFile? = null
            for (f in current.listFiles()) {
                val fname = f.name ?: ""
                if (f.isDirectory && !fname.startsWith(".") && fname.equals(part, ignoreCase = true)) {
                    nextDir = f
                    break
                }
            }
            if (nextDir == null) return null
            current = nextDir
        }
        return current
    }

    private fun scanFolderRecursively(
        dir: DocumentFile,
        outList: MutableList<MediaItem>,
        outMap: ConcurrentHashMap<Long, MediaItem>
    ) {
        val files = dir.listFiles()
        for (file in files) {
            val name = file.name ?: ""
            // Filter out hidden files and folders (starting with .)
            if (name.isBlank() || name.startsWith(".")) continue

            if (file.isDirectory) {
                scanFolderRecursively(file, outList, outMap)
            } else if (file.isFile) {
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = file.type ?: ""
                val isVideo = videoExtensions.contains(ext) || mime.startsWith("video/")

                if (isVideo && file.length() > 0) {
                    val id = (file.uri.toString().hashCode().toLong() and 0x7FFFFFFF)
                    val item = MediaItem(
                        id = id,
                        title = name,
                        uri = file.uri,
                        size = file.length(),
                        durationMs = 0L,
                        mimeType = getAccurateMimeType(name, mime),
                        dateModified = file.lastModified()
                    )
                    outList.add(item)
                    outMap[id] = item
                }
            }
        }
    }

    fun findMediaById(id: Long): MediaItem? {
        return mediaCache[id] ?: cachedList.find { it.id == id }
    }

    private fun getAccurateMimeType(name: String, fallbackMime: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mkv" -> "video/x-matroska"
            "mp4", "m4v" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "ts" -> "video/mp2t"
            "mov" -> "video/quicktime"
            "flv" -> "video/x-flv"
            else -> if (fallbackMime.isNotBlank()) fallbackMime else "video/mp4"
        }
    }
}
