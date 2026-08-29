package com.dwplayer.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.dwplayer.core.smb.SmbClientManager
import com.dwplayer.core.smb.SmbDataSourceFactory
import com.dwplayer.data.daos.PlaybackHistoryDao
import com.dwplayer.data.entities.PlaybackHistoryEntity
import com.dwplayer.data.entities.SmbShareEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@UnstableApi
class DwVideoPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbClientManager: SmbClientManager,
    private val playbackHistoryDao: PlaybackHistoryDao,
    val subtitlePreferencesManager: SubtitlePreferencesManager
) {
    companion object {
        private const val TAG = "DwVideoPlayer"
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    init {
        playerScope.launch {
            subtitlePreferencesManager.settings.collect { settings ->
                _uiState.update { it.copy(subtitleSettings = settings) }
            }
        }
    }

    val trackSelector: DefaultTrackSelector by lazy {
        DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setPreferredTextLanguage("fa").setPreferredAudioLanguage("en"))
        }
    }

    private val loadControl: DefaultLoadControl by lazy {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .build()
    }

    private val renderersFactory: DefaultRenderersFactory by lazy {
        DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setAllowedVideoJoiningTimeMs(5000)
            setEnableAudioFloatOutput(true)
            setEnableAudioTrackPlaybackParams(true)
        }
    }

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaUri: String = ""
    private var currentMediaTitle: String = ""
    private var isSmbMedia: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true, isLoading = false) }
                }
                Player.STATE_READY -> {
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            isLoading = false,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                            isPlaying = exoPlayer.isPlaying
                        )
                    }
                    extractTracks()
                }
                Player.STATE_ENDED -> {
                    _uiState.update { it.copy(isEnded = true, isPlaying = false) }
                    savePlaybackPosition(isCompleted = true)
                }
                Player.STATE_IDLE -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player Error", error)
            _uiState.update { it.copy(errorMessage = error.message ?: "Playback error", isLoading = false) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            extractTracks()
        }
    }

    fun playMedia(
        mediaUri: String,
        title: String,
        isSmb: Boolean = false,
        smbShare: SmbShareEntity? = null,
        smbFilePath: String? = null,
        authHeader: String? = null
    ) {
        currentMediaUri = mediaUri
        currentMediaTitle = title
        isSmbMedia = isSmb

        _uiState.update {
            it.copy(
                mediaUri = mediaUri,
                title = title,
                isLoading = true,
                errorMessage = null,
                isEnded = false
            )
        }

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val builtMediaItem = MediaItem.fromUri(mediaUri)

        val mediaSource: MediaSource = if (isSmb && smbShare != null && smbFilePath != null) {
            val smbFactory = SmbDataSourceFactory(smbClientManager, smbShare, smbFilePath)
            ProgressiveMediaSource.Factory(smbFactory, extractorsFactory)
                .createMediaSource(builtMediaItem)
        } else {
            val defaultFactory = if (!authHeader.isNullOrBlank()) {
                val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(mapOf("Authorization" to authHeader))
                DefaultDataSource.Factory(context, httpFactory)
            } else {
                DefaultDataSource.Factory(context)
            }
            DefaultMediaSourceFactory(defaultFactory, extractorsFactory)
                .createMediaSource(builtMediaItem)
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // Resume saved position if exists
        playerScope.launch(Dispatchers.IO) {
            val history = playbackHistoryDao.getHistory(mediaUri)
            if (history != null && history.lastPositionMs > 5000 && !history.isCompleted) {
                withContext(Dispatchers.Main) {
                    exoPlayer.seekTo(history.lastPositionMs)
                }
            }
        }

        startPositionTicker()
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = playerScope.launch {
            var saveCounter = 0
            while (isActive) {
                delay(500)
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    val currentPos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    val buffered = exoPlayer.bufferedPosition

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration,
                            bufferedPositionMs = buffered
                        )
                    }

                    saveCounter++
                    if (saveCounter >= 10) { // Every 5 seconds
                        saveCounter = 0
                        savePlaybackPosition(isCompleted = false)
                    }
                }
            }
        }
    }

    private fun savePlaybackPosition(isCompleted: Boolean) {
        if (currentMediaUri.isBlank()) return
        playerScope.launch(Dispatchers.IO) {
            val currentPos = withContext(Dispatchers.Main) { exoPlayer.currentPosition }
            val duration = withContext(Dispatchers.Main) { exoPlayer.duration.coerceAtLeast(0L) }
            val history = PlaybackHistoryEntity(
                mediaUri = currentMediaUri,
                title = currentMediaTitle,
                lastPositionMs = currentPos,
                durationMs = duration,
                lastPlayedAt = System.currentTimeMillis(),
                isCompleted = isCompleted,
                isSmb = isSmbMedia
            )
            playbackHistoryDao.insertOrUpdate(history)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekRelative(offsetMs: Long) {
        val target = (exoPlayer.currentPosition + offsetMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setResizeMode(mode: VideoResizeMode) {
        _uiState.update { it.copy(resizeMode = mode) }
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val parameters = trackSelector.buildUponParameters()
            .setOverrideForType(
                TrackSelectionOverride(
                    exoPlayer.currentTracks.groups[trackInfo.groupIndex].mediaTrackGroup,
                    trackInfo.trackIndex
                )
            )
            .build()
        trackSelector.setParameters(parameters)
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        if (trackInfo == null) {
            // Disable subtitles
            val parameters = trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            trackSelector.setParameters(parameters)
        } else {
            val parameters = trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(
                        exoPlayer.currentTracks.groups[trackInfo.groupIndex].mediaTrackGroup,
                        trackInfo.trackIndex
                    )
                )
                .build()
            trackSelector.setParameters(parameters)
        }
    }

    private fun extractTracks() {
        val tracks = exoPlayer.currentTracks
        val audioList = mutableListOf<TrackInfo>()
        val subtitleList = mutableListOf<TrackInfo>()
        var selectedAudioIdx = -1
        var selectedSubIdx = -1

        for (g in 0 until tracks.groups.size) {
            val group = tracks.groups[g]
            val trackType = group.type

            for (t in 0 until group.length) {
                val format = group.getTrackFormat(t)
                val isSelected = group.isTrackSelected(t)
                val label = format.label ?: format.language ?: "Track ${t + 1}"
                val lang = format.language

                val info = TrackInfo(
                    id = "${g}_$t",
                    groupIndex = g,
                    trackIndex = t,
                    label = label,
                    language = lang,
                    isSelected = isSelected
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(info)
                    if (isSelected) selectedAudioIdx = audioList.size - 1
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subtitleList.add(info)
                    if (isSelected) selectedSubIdx = subtitleList.size - 1
                }
            }
        }

        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subtitleList,
                currentAudioIndex = selectedAudioIdx,
                currentSubtitleIndex = selectedSubIdx
            )
        }
    }

    fun release() {
        savePlaybackPosition(isCompleted = false)
        positionJob?.cancel()
        playerScope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
