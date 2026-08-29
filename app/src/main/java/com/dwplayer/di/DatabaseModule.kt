package com.dwplayer.di

import android.content.Context
import androidx.room.Room
import com.dwplayer.data.DwDatabase
import com.dwplayer.data.daos.DownloadTaskDao
import com.dwplayer.data.daos.PlaybackHistoryDao
import com.dwplayer.data.daos.SmbShareDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DwDatabase {
        return Room.databaseBuilder(
            context,
            DwDatabase::class.java,
            "dwplayer.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDownloadTaskDao(database: DwDatabase): DownloadTaskDao = database.downloadTaskDao()

    @Provides
    fun provideSmbShareDao(database: DwDatabase): SmbShareDao = database.smbShareDao()

    @Provides
    fun providePlaybackHistoryDao(database: DwDatabase): PlaybackHistoryDao = database.playbackHistoryDao()

    @Provides
    fun providePlaylistDao(database: DwDatabase): com.dwplayer.data.daos.PlaylistDao = database.playlistDao()
}
