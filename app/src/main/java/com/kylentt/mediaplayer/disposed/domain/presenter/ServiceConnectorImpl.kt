package com.kylentt.mediaplayer.disposed.domain.presenter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.disposed.domain.presenter.util.State
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

// Application Context
class ServiceConnectorImpl(
    val context: Context,
    val coil: ImageLoader
) : MusicServiceConnector {

    // TODO: Find suitable usage of MediaBrowser

    private val _serviceState = MutableStateFlow<State.ServiceState>(State.ServiceState.Unit)
    val serviceState = _serviceState.asStateFlow()

    private val _playerPlaybackState = MutableStateFlow<State.PlayerState.PlayerPlaybackState>(State.PlayerState.PlayerPlaybackState.Unit)
    val playerPlaybackState = _playerPlaybackState.asStateFlow()

    private val _playerPlayState = MutableStateFlow<State.PlayerState.PlayerPlayState>(State.PlayerState.PlayerPlayState.PlayWhenReadyDisabled)
    val playerPlayState = _playerPlayState.asStateFlow()

    private val _playerItemState = MutableStateFlow<State.PlayerState.PlayerItemState>(State.PlayerState.PlayerItemState.CurrentlyPlaying(-1, MediaItem.EMPTY, emptyList()))
    val playerItemState = _playerItemState.asStateFlow()

    private val _position = MutableStateFlow(-1L)
    val position = _position.asStateFlow()

    private val _duration = MutableStateFlow(-1L)
    val duration = _position.asStateFlow()

    private suspend fun getPos() = withContext(Dispatchers.Main) {
        if (isServiceConnected()) mediaController.currentPosition else -1L
    }
    private suspend fun getDur() = withContext(Dispatchers.Main) {
        if (isServiceConnected()) mediaController.duration else -1L
    }

    // Media
    private var sessionToken: SessionToken? = null
    private var futureMediaController: ListenableFuture<MediaController>? = null
    private lateinit var mediaController: MediaController

    @MainThread
    override fun connectService(
        onConnected: (MediaController) -> Unit
    ) {
        if (isServiceConnected()) {
            onConnected(mediaController)
            return
        }
        _serviceState.value = State.ServiceState.Connecting

        sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        futureMediaController = MediaController.Builder(context, sessionToken!!).buildAsync()

        if (sessionToken != null && futureMediaController != null) {

            futureMediaController!!.addListener( {

                mediaController = futureMediaController!!.get()

                setupController(mediaController)
                onConnected(mediaController)
                _serviceState.value = State.ServiceState.Connected
            }, MoreExecutors.directExecutor())

        } else _serviceState.value = State.ServiceState.Error(NullPointerException("Couldn't Initialize Session"))
    }

    fun checkServiceState() {
        if (isServiceConnected()) {
            _serviceState.value = State.ServiceState.Connected
        } else _serviceState.value = State.ServiceState.Disconnected
    }

    // for VM and Service to Toast
    fun connectorToast(msg: String, long: Boolean) = Toast.makeText(context, msg, if (long)Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    private var fading = false
    suspend fun readyWithFade(listener: (MediaController) -> Unit) = withContext(Dispatchers.Main) {
        if (fading /*|| !isServiceConnected()*/) {
            Timber.d("ServiceConnector AudioEvent FadingAudio Skipped")
            controller(onReady = listener)
            return@withContext
        }
        fading = true

        while (mediaController.volume > 0.11f && mediaController.playWhenReady) {
            Timber.d("ServiceConnector AudioEvent FadingAudio ${mediaController.volume}")
            mediaController.volume = mediaController.volume -0.1f
            delay(100)
        }

        synchronized(fading) {
            listener(mediaController)
            controller(
                onReady = {
                    fading = false
                    it.volume = 1f
                }
            )
        }

        Timber.d("ServiceConnector Event AudioFaded ${mediaController.volume}")
    }

    suspend fun positionEmitter() = flow {
        while (true) {
            val pos = getPos()
            val dur = getDur()
            if (pos > -1 && pos <= dur) {
                _position.value = pos
                _duration.value = dur
                emit(true)
            } else emit(false)
            delay(1000)
        }
    }.conflate()


    override fun isServiceConnected(): Boolean {
        if (sessionToken == null) return false
        if (!::mediaController.isInitialized) return false
        return (mediaController.isConnected)
    }

    fun releaseSession() {
        sessionToken = null
        mediaController.release()
    }

    // whenever using controller, its already assumed that Service is already connected
    // so i'll delete the onConnect listener

    @MainThread
    fun controller(
        onBuffer: ((MediaController) -> Unit)? = null,
        onEnded: ((MediaController) -> Unit)? = null,
        onIdle: ((MediaController) -> Unit)? = null,
        onReady: ((MediaController) -> Unit)? = null,
    ) {
        onIdle?.let { whenIdle(it) }
        onBuffer?.let { whenBuffer(it) }
        onReady?.let { whenReady(it) }
        onEnded?.let { whenEnded(it) }
    }

    private fun setupController(controller: MediaController) {
        with(controller) {
            addListener( object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_IDLE -> {
                            controlIdle()
                            _playerPlaybackState.value = State.PlayerState.PlayerPlaybackState.Idle
                        }
                        Player.STATE_BUFFERING -> {
                            controlBuffer()
                            _playerPlaybackState.value = State.PlayerState.PlayerPlaybackState.Buffering
                        }
                        Player.STATE_READY -> {
                            controlReady()
                            _playerPlaybackState.value = State.PlayerState.PlayerPlaybackState.Ready
                        }
                        Player.STATE_ENDED -> {
                            controlEnded()
                            _playerPlaybackState.value = State.PlayerState.PlayerPlaybackState.Ended
                        }
                    }
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)
                    val list = mutableListOf<MediaItem>()
                    for (i in 0 until mediaController.mediaItemCount) {
                        list.add(mediaController.getMediaItemAt(i))
                    }
                    _playerItemState.value = State.PlayerState.PlayerItemState.CurrentlyPlaying(
                        mediaController.currentMediaItemIndex,
                        mediaController.currentMediaItem ?: MediaItem.EMPTY,
                        list
                    )
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    _playerPlayState.value =
                        if (playWhenReady) State.PlayerState.PlayerPlayState.PlayWhenReadyEnabled
                        else State.PlayerState.PlayerPlayState.PlayWhenReadyDisabled
                }
            })
        }
    }

    fun broadcast(intent: Intent) {
        context.sendBroadcast(intent)
    }

    private var controlReadyListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlBufferListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlEndedListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlIdleListener = mutableListOf<( (MediaController) -> Unit)>()

    // executed when Player.STATE changed
    private var lock = Any()
    private fun controlReady() = synchronized(lock) { controlReadyListener.forEach { it(mediaController) }
        controlReadyListener.clear()
    }
    private var lock1 = Any()
    private fun controlBuffer() = synchronized(lock1) { controlBufferListener.forEach { it(mediaController) }
        controlBufferListener.clear()
    }
    private var lock2 = Any()
    private fun controlEnded() = synchronized(lock2) { controlEndedListener.forEach { it(mediaController) }
        controlEndedListener.clear()
    }
    private var lock3 = Any()
    private fun controlIdle() = synchronized(lock3) { controlIdleListener.forEach { it(mediaController) }
        controlIdleListener.clear()
    }

    private fun isPlayerIdling() = mediaController.playbackState == Player.STATE_IDLE
    private fun isPlayerBuffering() = mediaController.playbackState == Player.STATE_BUFFERING
    private fun isPlayerReady() = mediaController.playbackState == Player.STATE_READY
    private fun isPlayerEnded() = mediaController.playbackState == Player.STATE_ENDED

    @MainThread
    private fun whenReady( command: (MediaController) -> Unit ) {
        if (isPlayerReady())
            command(mediaController) else { this.controlReadyListener.add(command) }
    }

    @MainThread
    private fun whenBuffer( command: ( MediaController) -> Unit  ) {
        if (isPlayerBuffering())
            command(mediaController) else controlBufferListener.add(command)
    }

    @MainThread
    private fun whenEnded(command: ( (MediaController) -> Unit ) ) {
        if (isPlayerEnded())
            command(mediaController) else controlEndedListener.add(command)
    }

    @MainThread
    private fun whenIdle( command: ( (MediaController) -> Unit ) ) {
        if (isPlayerIdling())
            command(mediaController) else controlReadyListener.add(command)
    }

    companion object {
        fun setServiceState(impl: ServiceConnectorImpl, state: State.ServiceState) {
            impl._serviceState.value = state
        }
    }
}