package com.dwplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.daos.PlaybackHistoryDao
import com.dwplayer.data.daos.PlaylistDao
import com.dwplayer.data.daos.SmbShareDao
import com.dwplayer.data.entities.DownloadTaskEntity
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.entities.PlaylistEntity
import com.dwplayer.data.entities.PlaylistItemEntity
import com.dwplayer.data.entities.SmbShareEntity

@Database(
    entities = [
        DownloadTaskEntity::class,
        SmbShareEntity::class,
        PlaybackHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DwDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun smbShareDao(): SmbShareDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
}
