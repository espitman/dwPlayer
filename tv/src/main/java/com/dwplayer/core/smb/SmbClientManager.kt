package com.dwplayer.core.smb

import android.util.Log
import com.dwplayer.data.entities.SmbShareEntity
import com.dwplayer.data.models.SmbItem
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File as SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbClientManager @Inject constructor() {
    companion object {
        private const val TAG = "SmbClientManager"
    }

    private val client: SMBClient by lazy {
        val config = SmbConfig.builder()
            .withTimeout(15, TimeUnit.SECONDS)
            .withSoTimeout(60, TimeUnit.SECONDS)
            .withMultiProtocolNegotiate(true)
            .build()
        SMBClient(config)
    }

    private val connectionCache = ConcurrentHashMap<String, Connection>()
    private val sessionCache = ConcurrentHashMap<String, Session>()

    suspend fun testConnection(
        host: String,
        shareName: String,
        username: String?,
        password: String?,
        domain: String?,
        port: Int = 445
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = client.connect(host, port)
            val authContext = if (!username.isNullOrBlank()) {
                AuthenticationContext(username, (password ?: "").toCharArray(), domain ?: "")
            } else {
                AuthenticationContext.anonymous()
            }
            val session = connection.authenticate(authContext)
            val share = session.connectShare(shareName) as DiskShare
            val isConnected = share.isConnected
            share.close()
            session.close()
            connection.close()

            if (isConnected) {
                Result.success("Connected successfully to smb://$host/$shareName")
            } else {
                Result.failure(Exception("Share could not be connected"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test SMB connection failed", e)
            Result.failure(e)
        }
    }

    suspend fun listDirectoryResult(shareEntity: SmbShareEntity, path: String): Result<List<SmbItem>> = withContext(Dispatchers.IO) {
        try {
            val diskShare = getDiskShare(shareEntity)
            val normalizedPath = path.replace("/", "\\").trim('\\')
            val list = mutableListOf<SmbItem>()
            val fileList = diskShare.list(normalizedPath)
            for (f in fileList) {
                val fileName = f.fileName
                // Filter out empty or hidden files/folders (starting with .)
                if (fileName.isNullOrBlank() || fileName.startsWith(".")) continue

                val isDir = EnumWithValue.EnumUtils.isSet(f.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
                val itemPath = if (normalizedPath.isEmpty()) fileName else "$normalizedPath\\$fileName"

                list.add(
                    SmbItem(
                        name = fileName,
                        path = itemPath.replace("\\", "/"),
                        isDirectory = isDir,
                        size = f.endOfFile,
                        lastModified = f.changeTime?.toEpochMillis() ?: 0L
                    )
                )
            }
            Result.success(list.sortedWith(compareByDescending<SmbItem> { it.isDirectory }.thenBy { it.name.lowercase() }))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list SMB directory $path", e)
            Result.failure(e)
        }
    }

    suspend fun listDirectory(shareEntity: SmbShareEntity, path: String): List<SmbItem> =
        listDirectoryResult(shareEntity, path).getOrDefault(emptyList())

    fun openFileForStreaming(shareEntity: SmbShareEntity, path: String): SmbFile {
        val diskShare = getDiskShare(shareEntity)
        val normalizedPath = path.replace("/", "\\").trim('\\')
        return diskShare.openFile(
            normalizedPath,
            setOf(AccessMask.GENERIC_READ),
            setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            setOf(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
    }

    @Synchronized
    private fun getDiskShare(shareEntity: SmbShareEntity): DiskShare {
        val key = "${shareEntity.host}_${shareEntity.shareName}_${shareEntity.username}"
        var session = sessionCache[key]
        var connection = connectionCache[key]

        if (connection == null || !connection.isConnected || session == null) {
            connection = client.connect(shareEntity.host, shareEntity.port)
            val auth = if (!shareEntity.username.isNullOrBlank()) {
                AuthenticationContext(shareEntity.username, (shareEntity.password ?: "").toCharArray(), shareEntity.domain ?: "")
            } else {
                AuthenticationContext.anonymous()
            }
            session = connection.authenticate(auth)
            connectionCache[key] = connection
            sessionCache[key] = session
        }
        val activeSession = session ?: throw IllegalStateException("SMB Session could not be established")
        return activeSession.connectShare(shareEntity.shareName) as DiskShare
    }
}
