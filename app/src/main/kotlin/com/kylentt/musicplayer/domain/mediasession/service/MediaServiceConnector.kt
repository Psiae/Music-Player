package com.kylentt.musicplayer.domain.mediasession.service

import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.core.exoplayer.util.toStrState
import com.kylentt.mediaplayer.core.exoplayer.util.toStrTransitionReason
import com.kylentt.mediaplayer.core.util.handler.getDisplayTitle
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicService
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber


sealed class MediaServiceState {
    object UNIT : MediaServiceState()
    object DISCONNECTED : MediaServiceState()
    object CONNECTING : MediaServiceState()
    object CONNECTED : MediaServiceState()
    data class ERROR(val msg: String, val e: Exception) : MediaServiceState()
}

sealed class PlaybackState {

    abstract fun toStrState(): String

    object UNIT : PlaybackState() {
        override fun toStrState(): String = "UNIT"
    }
    object BUFFERING : PlaybackState() {
        override fun toStrState(): String = Player.STATE_BUFFERING.toStrState()
    }
    object READY: PlaybackState() {
        override fun toStrState(): String = Player.STATE_READY.toStrState()
    }
    object IDLE: PlaybackState() {
        override fun toStrState(): String = Player.STATE_IDLE.toStrState()
    }
    object ENDED: PlaybackState() {
        override fun toStrState(): String = Player.STATE_ENDED.toStrState()
    }

}

sealed class ControllerCommand {

    object Unit: ControllerCommand()
    object Prepare : ControllerCommand()
    object Stop : ControllerCommand()
    data class MultiCommand(val commands: List<ControllerCommand>) : ControllerCommand()
    data class SeekToIndex(val index: Int, val pos: Long = 0) : ControllerCommand()
    data class SeekToPos(val pos: Long) : ControllerCommand()
    data class SeekToPercent(val percent: Float) : ControllerCommand()
    data class SetMediaItem(val item: MediaItem) : ControllerCommand()
    data class SetMediaItems(val items: List<MediaItem>,val startIndex: Int = 0, val startPos: Long = 0L) : ControllerCommand()
    data class SetPlayWhenReady(val play: Boolean) : ControllerCommand()
    data class WhenReady(val command: ControllerCommand) : ControllerCommand()

    data class WithFade(
        val command: ControllerCommand,
        @FloatRange(from = 0.0, to =1.0 ) val to: Float = 0f,
        val flush: Boolean = false
    ) : ControllerCommand()
}

@MainThread
internal class MediaServiceConnector(
    private val manager: MediaSessionManager,
    private val context: Context
) {

    var mediaServiceController = MediaServiceController()
        private set

    private val _playerState = MutableStateFlow<PlaybackState>(PlaybackState.UNIT)
    val playerState = _playerState.asStateFlow()

    private val _playerMediaItem = MutableStateFlow<MediaItem>(MediaItem.EMPTY)
    val playerMediaItem = _playerMediaItem.asStateFlow()

    private val _serviceState = MutableStateFlow<MediaServiceState>(MediaServiceState.UNIT)
    val serviceState = _serviceState.asStateFlow()

    private lateinit var sessionToken: SessionToken
    private lateinit var futureMediaController: ListenableFuture<MediaController>
    private lateinit var mediaController: MediaController

    private fun isControllerConnected(): Boolean = when {
        !::mediaController.isInitialized  -> false
        else -> mediaController.isConnected
    }

    private val connectLock = Any()
    private fun connectService( onConnected: (MediaController) -> Unit ) {
        synchronized(connectLock) {
            if (isControllerConnected()) {
                onConnected(mediaController)
                return
            }
            if (serviceState.value is MediaServiceState.CONNECTING) {
                futureMediaController.addListener( { onConnected(mediaController) }, MoreExecutors.directExecutor())
                return
            }

            _serviceState.value = MediaServiceState.CONNECTING

            sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
            futureMediaController = MediaController.Builder(context, sessionToken)
                .setApplicationLooper(Looper.getMainLooper())
                .buildAsync()
                .apply { addListener( { with(this.get()) {
                    mediaController = this
                    mediaServiceController.provideController(mediaController)
                    onConnected(mediaController)
                    _serviceState.value = MediaServiceState.CONNECTED
                } }, MoreExecutors.directExecutor() ) }
        }
    }

    inner class MediaServiceController {

        private lateinit var controller: MediaController

        fun provideController(controller: MediaController) {
            this.controller = controller
            setupController(controller)
        }

        private var commandLock = Any()
        fun commandController(command: ControllerCommand) {
            if (!::controller.isInitialized) {
                connectService { commandController(command) }
                return
            }
            if (!controller.isConnected) {
                connectService { commandController(command) }
                return
            }
            when(command) {
                ControllerCommand.Prepare -> {
                    mediaController.prepare()
                }
                ControllerCommand.Stop -> {
                    mediaController.stop()
                }
                is ControllerCommand.MultiCommand -> {
                    command.commands.forEach { commandController(it) }
                }
                is ControllerCommand.SeekToIndex -> {
                    mediaController.seekTo(command.index, command.pos)
                }
                is ControllerCommand.SeekToPercent -> {
                    mediaController.seekTo(((mediaController.currentPosition.toFloat() / 100) / command.percent).toLong())
                }
                is ControllerCommand.SeekToPos -> {
                    mediaController.seekTo(command.pos)
                }
                is ControllerCommand.SetMediaItem -> {
                    mediaController.setMediaItem(command.item)
                }
                is ControllerCommand.SetMediaItems -> {
                    mediaController.setMediaItems(command.items, command.startIndex, command.startPos)
                }
                is ControllerCommand.SetPlayWhenReady -> {
                    mediaController.playWhenReady = command.play
                }
                is ControllerCommand.WhenReady -> {
                    if (command.command is ControllerCommand.WhenReady) {
                        Timber.e("Invalid Command $command ${command.command}")
                        return
                    }
                    whenReady { commandController(command.command) }
                }
                is ControllerCommand.WithFade -> {
                    scope.launch { exoFade(command) }
                }
                ControllerCommand.Unit -> { Unit }
            }
        }

        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        private val controlListenerLock = Any()

        private val controlIdleListener = mutableListOf< (MediaController) -> Unit >()
        private val controlBufferListener = mutableListOf< (MediaController) -> Unit >()
        private val controlReadyListener = mutableListOf< (MediaController) -> Unit >()
        private val controlEndedListener = mutableListOf< (MediaController) -> Unit >()

        private fun whenReady(listener: (MediaController) -> Unit ) {
            Timber.d("whenReady with current playbackState ${mediaController.playbackState.toStrState()}")
            if (mediaController.playbackState == Player.STATE_READY) {
                listener(mediaController)
            } else {
                controlReadyListener.add(listener)
            }
        }

        private fun whenBuffer(listener: (MediaController) -> Unit ) {
            if (mediaController.playbackState == Player.STATE_READY) {
                listener(mediaController)
            } else {
                controlReadyListener.add(listener)
            }
        }

        private fun triggerListener(state: PlaybackState): Unit = synchronized(controlListenerLock) {
            _playerState.value = state
            when(state) {
                is PlaybackState.BUFFERING -> {
                    controlBufferListener.forEach { it(mediaController) }
                    controlBufferListener.clear()
                }
                is PlaybackState.ENDED -> {
                    controlEndedListener.forEach { it(mediaController) }
                    controlEndedListener.clear()
                }
                is PlaybackState.IDLE -> {
                    controlIdleListener.forEach { it(mediaController) }
                    controlIdleListener.clear()
                }
                is PlaybackState.READY -> {
                    controlReadyListener.forEach { it(mediaController) }
                    controlReadyListener.clear()
                }
                is PlaybackState.UNIT -> {
                    // we should never be here
                    Timber.e("triggerListener with $state")
                }
            }
        }

        private fun setupController(controller: MediaController) {

            with(controller) {

                addListener( object : Player.Listener {

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        Timber.d("onMediaItemTransition $reason ${reason.toStrTransitionReason()}")
                        when(reason) {
                            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                                if (mediaItem?.localConfiguration == null) _playerMediaItem.value = mediaItem ?: MediaItem.EMPTY
                            }
                            else -> { _playerMediaItem.value = mediaItem ?: MediaItem.EMPTY }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        when (playbackState) {
                            Player.STATE_IDLE -> { triggerListener(PlaybackState.IDLE) }
                            Player.STATE_BUFFERING -> { triggerListener(PlaybackState.BUFFERING) }
                            Player.STATE_READY -> { triggerListener(PlaybackState.READY) }
                            Player.STATE_ENDED -> { triggerListener(PlaybackState.ENDED) }
                        }
                    }
                })
            }
        }
        private var fading = false
        private val fadeLock = Any()
        private var fadeListener = mutableListOf<ControllerCommand.WithFade>()

        private suspend fun exoFade(
            command: ControllerCommand.WithFade
        ) {
            val player = mediaController
            val to = command.to

            if (command.flush) fadeListener.clear()
            fadeListener.add(command)

            if (fading || !player.isPlaying) {
                fading = false
                synchronized(fadeLock) {
                    fadeListener.forEach { commandController(it.command) }
                    fadeListener.clear()
                    whenReady { player.volume = 1f }
                }
                return
            }

            fading = true
            while (player.volume > to && fading) {
                if (player.volume > 0.15f) player.volume -= 0.15f else player.volume -= player.volume
                delay(100)
            }

            if (fading) {
                fading = false
                synchronized(fadeLock) {
                    fadeListener.forEach { commandController(it.command) }
                    fadeListener.clear()
                    whenReady { player.volume = 1f }
                }
                return
            }
        }
    }
}