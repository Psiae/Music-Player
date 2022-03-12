package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import timber.log.Timber

/** Any Exoplayer Utility should be in this package*/

/*
object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> {
                Timber.d("Event PlaybackState STATE_IDLE")
                exoIdle()
                // TODO Something to do while Idling
            }
            Player.STATE_BUFFERING -> {
                Timber.d("Event PlaybackState STATE_BUFFERING")
                exoBuffer()
                // TODO Something to do while Buffering
            }
            Player.STATE_READY -> {
                Timber.d("Event PlaybackState STATE_READY")
                exoReady()
                // TODO Another thing to do when Ready
            }
            Player.STATE_ENDED -> {
                Timber.d("MusicService Event PlaybackState STATE_ENDED")
                exoEnded()
                exo.pause()
                // TODO Another thing to do when ENDED
            }
        }
        */
/*mSession?.let { onUpdateNotification(it) }*//*

    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        mSession?.let { onUpdateNotification(it) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        mSession?.let { onUpdateNotification(it) }
    }

    var retry = true
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        if (error.errorCodeName == "ERROR_CODE_DECODING_FAILED") {
            controller(
                whenReady = { retry = true }
            ) {
                it.stop()
                it.prepare()
            }
        }
        if (error.errorCodeName == "ERROR_CODE_IO_FILE_NOT_FOUND") {
            controller {
                it.removeMediaItem(it.currentMediaItemIndex)
                it.pause()
                it.prepare()
            }
        }
        Timber.d("MusicService PlaybackException onPlayerError ${error.errorCodeName}")
        serviceToast("Unable to Play This Song ${error.errorCodeName}")
    }
}*/
