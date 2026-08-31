package com.dwplayer.core.downloader

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.media.MediaMetadataRetriever
import com.dwplayer.data.models.StorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "dwplayer_prefs"
        private const val KEY_CUSTOM_STORAGE = "custom_storage_path"
        private const val DEFAULT_FOLDER = "dwPlayerMovies"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDownloadDirectory(): File {
        val custom = prefs.getString(KEY_CUSTOM_STORAGE, null)
        if (!custom.isNullOrBlank()) {
            val f = File(custom)
            if (f.exists() && f.canWrite()) return f
        }

        // Default: Movies/dwPlayerMovies on External Storage or App Files Dir
        val externalMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val target = if (externalMovies != null && (externalMovies.exists() || externalMovies.mkdirs())) {
            File(externalMovies, DEFAULT_FOLDER)
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, DEFAULT_FOLDER)
        }

        if (!target.exists()) {
            target.mkdirs()
        }
        return target
    }

    fun setCustomStoragePath(path: String) {
        prefs.edit().putString(KEY_CUSTOM_STORAGE, path).apply()
    }

    fun getStorageInfo(): StorageInfo {
        val dir = getDownloadDirectory()
        return try {
            val stat = StatFs(dir.absolutePath)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes
            val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            StorageInfo(
                freeSpace = formatBytes(freeBytes),
                totalSpace = formatBytes(totalBytes),
                usedPercent = usedPercent,
                path = dir.absolutePath
            )
        } catch (e: Exception) {
            StorageInfo(
                freeSpace = "Unknown",
                totalSpace = "Unknown",
                usedPercent = 0,
                path = dir.absolutePath
            )
        }
    }

    fun getLocalArchiveFiles(): List<com.dwplayer.data.models.LocalArchiveFile> {
        val dir = getDownloadDirectory()
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        val videoExts = setOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "flv", "wmv", "3gp", "vob", "iso")

        return files
            .filter { it.isFile && !it.name.startsWith(".") }
            .filter { file ->
                file.extension.lowercase() in videoExts
            }
            .map { file ->
                val ext = file.extension.lowercase()
                val metadata = readVideoMetadata(file)
                com.dwplayer.data.models.LocalArchiveFile(
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    sizeFormatted = formatBytes(file.length()),
                    lastModified = file.lastModified(),
                    extension = ext.uppercase(),
                    durationMs = metadata.durationMs,
                    videoWidth = metadata.width,
                    videoHeight = metadata.height
                )
            }
            .sortedByDescending { it.lastModified }
    }

    fun deleteLocalArchiveFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format("%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    private fun readVideoMetadata(file: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            VideoMetadata(
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                    ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                    ?: 0
            )
        } catch (_: Exception) {
            VideoMetadata()
        } finally {
            runCatching { retriever.release() }
        }
    }

    private data class VideoMetadata(
        val durationMs: Long = 0L,
        val width: Int = 0,
        val height: Int = 0
    )
}
