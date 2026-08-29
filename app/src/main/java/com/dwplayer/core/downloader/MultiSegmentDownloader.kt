package com.dwplayer.core.downloader

import android.util.Log
import com.dwplayer.data.models.DownloadProgressInfo
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class MultiSegmentDownloader(
    private val taskId: String,
    private val url: String,
    private val targetFolder: File,
    private val customFileName: String?,
    private val okHttpClient: OkHttpClient,
    private val onProgress: (DownloadProgressInfo) -> Unit,
    private val onComplete: (File) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "MultiSegmentDownloader"
        private const val NUM_SEGMENTS = 4
        private const val BUFFER_SIZE = 64 * 1024 // 64KB buffer
    }

    private val isCancelled = AtomicBoolean(false)
    private var downloadJob: Job? = null
    private val totalDownloadedBytes = AtomicLong(0L)

    fun start(scope: CoroutineScope) {
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                executeDownload()
            } catch (e: CancellationException) {
                Log.d(TAG, "Task $taskId was cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Task $taskId error", e)
                onError(e.message ?: "Download failed")
            }
        }
    }

    fun pause() {
        isCancelled.set(true)
        downloadJob?.cancel()
    }

    private suspend fun executeDownload() = withContext(Dispatchers.IO) {
        // Step 1: Probe the remote URL
        val headRequest = Request.Builder().url(url).head().build()
        var contentLength = -1L
        var acceptRanges = false
        var resolvedFileName = customFileName

        try {
            okHttpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    val rangesHeader = response.header("Accept-Ranges")
                    acceptRanges = rangesHeader?.contains("bytes", ignoreCase = true) == true

                    if (resolvedFileName.isNullOrBlank()) {
                        val disposition = response.header("Content-Disposition")
                        resolvedFileName = extractFileNameFromDisposition(disposition)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "HEAD request failed, falling back to GET: ${e.message}")
        }

        if (resolvedFileName.isNullOrBlank()) {
            resolvedFileName = extractFileNameFromUrl(url)
        }

        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val finalName = resolvedFileName ?: extractFileNameFromUrl(url)
        val destinationFile = File(targetFolder, finalName)
        val tempFile = File(targetFolder, "$finalName.dwpart")

        if (contentLength > 0 && acceptRanges && contentLength > 10 * 1024 * 1024) {
            // Multi-segment parallel download
            downloadMultiSegment(tempFile, destinationFile, contentLength)
        } else {
            // Single stream direct download (progressive / fallback)
            downloadSingleStream(tempFile, destinationFile, contentLength)
        }
    }

    private suspend fun downloadMultiSegment(
        tempFile: File,
        destinationFile: File,
        totalBytes: Long
    ) = coroutineScope {
        val segmentSize = totalBytes / NUM_SEGMENTS
        val segmentJobs = mutableListOf<Job>()
        val startTimes = System.currentTimeMillis()
        var lastSpeedCalcTime = startTimes
        var lastDownloadedBytes = 0L

        // Prepare file size
        RandomAccessFile(tempFile, "rw").use { raf ->
            if (raf.length() < totalBytes) {
                raf.setLength(totalBytes)
            }
        }

        for (i in 0 until NUM_SEGMENTS) {
            val startByte = i * segmentSize
            val endByte = if (i == NUM_SEGMENTS - 1) totalBytes - 1 else (startByte + segmentSize - 1)

            val job = launch(Dispatchers.IO) {
                downloadSegment(tempFile, startByte, endByte)
            }
            segmentJobs.add(job)
        }

        // Progress monitor loop
        val progressJob = launch(Dispatchers.IO) {
            while (isActive && !isCancelled.get()) {
                delay(500)
                val currentBytes = totalDownloadedBytes.get()
                val now = System.currentTimeMillis()
                val timeDiff = (now - lastSpeedCalcTime) / 1000.0

                val bytesDiff = currentBytes - lastDownloadedBytes
                val speedBps = if (timeDiff > 0) (bytesDiff / timeDiff).toLong() else 0L

                lastSpeedCalcTime = now
                lastDownloadedBytes = currentBytes

                val progress = if (totalBytes > 0) ((currentBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0
                val timeRemaining = calculateEta(totalBytes - currentBytes, speedBps)
                val etaTimestamp = if (speedBps > 0) now + ((totalBytes - currentBytes) / speedBps) * 1000 else null

                onProgress(
                    DownloadProgressInfo(
                        id = taskId,
                        progress = progress.coerceIn(0, 100),
                        downloadedBytes = currentBytes,
                        totalBytes = totalBytes,
                        speed = formatSpeed(speedBps),
                        speedBytesPerSec = speedBps,
                        timeRemaining = timeRemaining,
                        etaTimestamp = etaTimestamp
                    )
                )
            }
        }

        segmentJobs.joinAll()
        progressJob.cancel()

        if (!isCancelled.get()) {
            // Rename temp file to final destination
            if (destinationFile.exists()) destinationFile.delete()
            tempFile.renameTo(destinationFile)
            onComplete(destinationFile)
        }
    }

    private fun downloadSegment(tempFile: File, startByte: Long, endByte: Long) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$startByte-$endByte")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Segment HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body for segment")
            val inputStream = body.byteStream()
            val buffer = ByteArray(BUFFER_SIZE)

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(startByte)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled.get()) break
                    raf.write(buffer, 0, bytesRead)
                    totalDownloadedBytes.addAndGet(bytesRead.toLong())
                }
            }
        }
    }

    private suspend fun downloadSingleStream(
        tempFile: File,
        destinationFile: File,
        totalBytes: Long
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val resolvedTotal = if (totalBytes > 0) totalBytes else body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = tempFile.outputStream()

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var downloaded = 0L
            var lastCalcTime = System.currentTimeMillis()
            var lastBytes = 0L

            outputStream.use { out ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled.get()) break
                    out.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    totalDownloadedBytes.set(downloaded)

                    val now = System.currentTimeMillis()
                    if (now - lastCalcTime >= 500) {
                        val timeDiff = (now - lastCalcTime) / 1000.0
                        val bytesDiff = downloaded - lastBytes
                        val speedBps = if (timeDiff > 0) (bytesDiff / timeDiff).toLong() else 0L

                        lastCalcTime = now
                        lastBytes = downloaded

                        val progress = if (resolvedTotal > 0) ((downloaded.toDouble() / resolvedTotal.toDouble()) * 100).toInt() else 0
                        val timeRemaining = calculateEta(resolvedTotal - downloaded, speedBps)
                        val etaTimestamp = if (speedBps > 0 && resolvedTotal > downloaded) now + ((resolvedTotal - downloaded) / speedBps) * 1000 else null

                        onProgress(
                            DownloadProgressInfo(
                                id = taskId,
                                progress = progress.coerceIn(0, 100),
                                downloadedBytes = downloaded,
                                totalBytes = resolvedTotal,
                                speed = formatSpeed(speedBps),
                                speedBytesPerSec = speedBps,
                                timeRemaining = timeRemaining,
                                etaTimestamp = etaTimestamp
                            )
                        )
                    }
                }
            }

            if (!isCancelled.get()) {
                if (destinationFile.exists()) destinationFile.delete()
                tempFile.renameTo(destinationFile)
                onComplete(destinationFile)
            }
        }
    }

    private fun extractFileNameFromDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        return try {
            val filenamePattern = """filename\*?=['"]?(?:UTF-8'')?([^"';]+)['"]?""".toRegex(RegexOption.IGNORE_CASE)
            val match = filenamePattern.find(disposition)
            val name = match?.groupValues?.get(1)
            name?.let { URLDecoder.decode(it, "UTF-8") }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = url.substringBefore("?").substringBefore("#")
            val rawName = path.substringAfterLast("/")
            if (rawName.isNotBlank() && rawName.contains(".")) {
                URLDecoder.decode(rawName, "UTF-8")
            } else {
                "movie_${System.currentTimeMillis()}.mp4"
            }
        } catch (e: Exception) {
            "movie_${System.currentTimeMillis()}.mp4"
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        return when {
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            else -> String.format("%.2f GB/s", bytesPerSec / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun calculateEta(remainingBytes: Long, speedBps: Long): String {
        if (remainingBytes <= 0 || speedBps <= 0) return ""
        val seconds = remainingBytes / speedBps
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
