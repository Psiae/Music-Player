package com.kylentt.mediaplayer.core.exoplayer

import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.disposed.domain.service.MusicService
import kotlinx.coroutines.delay
import timber.log.Timber

@MainThread
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
                Player.STATE_ENDED -> {
                    exo.pause()
                    exoEnded()
                }
            }
            if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_BUFFERING) {
                notificationManager.updateNotification("playbackStateChanged !Idle !Buffer",false)
            }
        }
    }

    private var onMediaItemTransition = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                    notificationManager.updateNotification("MediaItemTransition Auto",true, shouldChangeBitmap = true)
                }
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                    notificationManager.updateNotification("MediaItemTransition PlaylistChanged",true, shouldChangeBitmap = true)
                }
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                    notificationManager.updateNotification("MediaItemTransition Repeat",true, shouldChangeBitmap = false)
                }
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                    notificationManager.updateNotification("MediaItemTransition SeekTo",true, shouldChangeBitmap = true)
                }
            }
        }
    }

    private var playWhenReadyChanged = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            when (reason) {

                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                    notificationManager.updateNotification( caller = "playWhenReadyChanged User Request to $playWhenReady",pp = playWhenReady )
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> {
                // TODO()
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> {
                    notificationManager.updateNotification( caller = "playWhenReadyChanged AudioBecomingNoisy $playWhenReady",pp = playWhenReady )
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                    notificationManager.updateNotification( caller = "playWhenReadyChanged AudioFocusLoss $playWhenReady",pp = playWhenReady )
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> {
                // TODO()
                }
            }
        }
    }

    private var repeatModeChangeListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            val repeat = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> {
                    "Repeat_Mode_All"
                }
                Player.REPEAT_MODE_OFF -> {
                    "Repeat_Mode_Off"
                }
                Player.REPEAT_MODE_ONE -> {
                    "Repeat_Mode_One"
                }
                else -> "Repeat_Mode_Unknown"
            }
            notificationManager.updateNotification("onRepeatModeChanged $repeat")
        }
    }

    private var playerErrorListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Timber.e(error)
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
        exo.addListener(playWhenReadyChanged)
        exo.addListener(repeatModeChangeListener)
        exo.addListener(playerErrorListener)
        exo.addListener(onMediaItemTransition)
    }

    companion object {
        fun getNotificationManager(controller: ExoController) = controller.notificationManager
    }
}



