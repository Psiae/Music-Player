package com.kylentt.mediaplayer.core.exoplayer

import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.disposed.domain.service.MusicService
import kotlinx.coroutines.delay

class ExoController(
    private val mediaSession: MediaSession,
    private val service: MediaLibraryService
) {

    // TODO move Notification Manager here

    private val notificationManager: ExoNotificationManager = ExoNotificationManager(service as MusicService)
    private val exo = mediaSession.player as ExoPlayer

    private val exoIdleListener = mutableListOf<( (ExoPlayer) -> Unit)>()
    private val exoBufferListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private val exoReadyListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private val exoEndedListener = mutableListOf<( (ExoPlayer) -> Unit )>()

    private fun exoReady() = synchronized(exoReadyListener) {
        exoReadyListener.forEach { it(exo) }
        exoReadyListener.clear()
    }
    private fun exoBuffer() = synchronized(exoBufferListener) {
        exoBufferListener.forEach { it(exo) }
        exoBufferListener.clear()
    }
    private fun exoEnded() = synchronized(exoEndedListener) {
        exoEndedListener.forEach { it(exo) }
        exoEndedListener.clear()
    }
    private fun exoIdle() = synchronized(exoIdleListener) {
        exoIdleListener.forEach { it(exo) }
        exoIdleListener.clear()
    }

    private fun whenExoReady(command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_READY)
            command(exo) else  exoReadyListener.add(command)
    }

    private fun whenBuffer( command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_BUFFERING)
            command(exo) else exoBufferListener.add(command)
    }

    private fun whenEnded(command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_ENDED)
            command(exo) else exoEndedListener.add(command)
    }

    private fun whenIdle( command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_IDLE)
            command(exo) else exoReadyListener.add(command)
    }

    private var playbackChangeListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when(playbackState) {
                Player.STATE_IDLE -> exoIdle()
                Player.STATE_BUFFERING -> exoBuffer()
                Player.STATE_READY -> exoReady()
                Player.STATE_ENDED -> exoEnded()
            }
            service.onUpdateNotification(mediaSession)
        }
    }

    private var isPlayingChangeListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            service.onUpdateNotification(mediaSession)
        }
    }

    private var repeatModeChangeListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            service.onUpdateNotification(mediaSession)
        }
    }

    private var playerErrorListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            when(error.errorCodeName) {
                "ERROR_CODE_DECODING_FAILED" -> {
                    controller {
                        it.stop()
                        it.prepare()
                    }
                }
                "ERROR_CODE_IO_FILE_NOT_FOUND" -> {
                    controller {
                        it.removeMediaItem(it.currentMediaItemIndex)
                        it.pause()
                        it.prepare()
                    }
                }
            }
        }
    }

    @MainThread
    fun controller(
        whenReady: ( (ExoPlayer) -> Unit)? = null,
        command: ( (ExoPlayer) -> Unit) = {}
    ) {
        command(exo)
        whenReady?.let { whenExoReady(it) }
    }

    private var fading = false
    private var fadeListener = mutableListOf< (ExoPlayer) -> Unit >()
    suspend fun exoFade(
        @FloatRange(from = 0.0,to = 1.0) to: Float,
        clear: Boolean,
        command: (ExoPlayer) -> Unit
    ) {
        if (clear) fadeListener.clear()
        fadeListener.add(command)

        if (fading) {
            fading = false
            controller(
                whenReady = {
                    it.volume = 1f
                }
            ) { exo ->
                synchronized(fadeListener) {
                    fadeListener.forEach { it(exo) }
                    fadeListener.clear()
                }
            }
        }

        fading = true
        while (exo.volume > to && exo.playWhenReady && fading) {
            exo.volume -= 0.1f
            delay(100)
        }

        if (fading) {
            controller(
                whenReady = {
                    exo.volume = 1f
                    fading = false
                }
            ) { exo ->
                synchronized(fadeListener) {
                    fadeListener.forEach { it(exo) }
                    fadeListener.clear()
                }
            }
        }
    }

    init {
        exo.addListener(playbackChangeListener)
        exo.addListener(isPlayingChangeListener)
        exo.addListener(repeatModeChangeListener)
        exo.addListener(playerErrorListener)
    }

    companion object {
        fun getNotificationManager(controller: ExoController) = controller.notificationManager
    }
}



