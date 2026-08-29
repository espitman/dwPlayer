package com.dwplayer.core.smb

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.dwplayer.data.entities.SmbShareEntity
import com.hierynomus.smbj.share.File as SmbFile
import java.io.EOFException
import java.io.IOException

class SmbDataSource(
    private val smbClientManager: SmbClientManager,
    private val shareEntity: SmbShareEntity,
    private val filePath: String
) : BaseDataSource(/* isNetwork = */ true) {

    private var dataSpec: DataSpec? = null
    private var smbFile: SmbFile? = null
    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        try {
            val file = smbClientManager.openFileForStreaming(shareEntity, filePath)
            this.smbFile = file
            val fileLength = file.fileInformation.standardInformation.endOfFile

            if (dataSpec.position > fileLength) {
                throw EOFException("Position ${dataSpec.position} exceeds file length $fileLength")
            }

            this.currentPosition = dataSpec.position
            this.bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - dataSpec.position
            }

            this.opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            throw IOException("Failed to open SMB stream: ${e.message}", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val file = smbFile ?: throw IOException("SmbFile is null")

        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            Math.min(bytesRemaining, length.toLong()).toInt()
        } else {
            length
        }

        val bytesRead = file.read(buffer, currentPosition, offset, bytesToRead)
        if (bytesRead <= 0) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong() && bytesRemaining > 0) {
                throw EOFException()
            }
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun close() {
        if (opened) {
            opened = false
            try {
                smbFile?.close()
            } catch (e: Exception) {
                // Ignore close exception
            } finally {
                smbFile = null
                transferEnded()
            }
        }
    }
}

class SmbDataSourceFactory(
    private val smbClientManager: SmbClientManager,
    private val shareEntity: SmbShareEntity,
    private val filePath: String
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SmbDataSource(smbClientManager, shareEntity, filePath)
    }
}
