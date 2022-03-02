package com.kylentt.mediaplayer.domain.presenter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.media.session.PlaybackState.STATE_PLAYING
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
import com.kylentt.mediaplayer.domain.service.MusicService
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
) : ServiceConnector {

    // TODO: Find suitable usage of MediaBrowser

    private val _isPlaying = MutableStateFlow<Boolean?>(null)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playerIndex = MutableStateFlow<Int?>(null)
    val playerIndex = _playerIndex.asStateFlow()

    private val _mediaItem = MutableStateFlow<MediaItem?>(null)
    val mediaItem = _mediaItem.asStateFlow()

    private val _mediaItems = MutableStateFlow<List<MediaItem>?>(null)
    val mediaItems = _mediaItems.asStateFlow()

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

    // for VM and Service to Toast
    fun connectorToast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()


    private var fading = false
    suspend fun readyWithFade(listener: (MediaController) -> Unit) {
        if (fading || !isServiceConnected()) {
            Timber.d("AudioEvent FadingAudio Skipped")
            controller(onConnected = listener)
            return
        }
        fading = true

        while (mediaController.volume > 0.1f && mediaController.playWhenReady) {
            Timber.d("AudioEvent FadingAudio ${mediaController.volume}")
            mediaController.volume = mediaController.volume -0.1f
            delay(100)
        }

        controller(
            onConnected = listener,
            onReady = {
                fading = false
                it.volume = 1f
            }
        )

        Timber.d("MusicService Event AudioFaded ${mediaController.volume}")
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

    // Media
    private var sessionToken: SessionToken? = null
    private lateinit var futureMediaController: ListenableFuture<MediaController>
    private lateinit var mediaController: MediaController
    private val _mediaController: MediaController?
        get() = if (futureMediaController.isDone) futureMediaController.get() else null

    override fun isServiceConnected(): Boolean {
        if (sessionToken == null) return false
        if (!::mediaController.isInitialized) return false
        return (mediaController.isConnected)
    }

    fun releaseSession() {
        sessionToken = null
        mediaController.release()
    }

    @MainThread
    fun controller(
        onBuffer: (MediaController) -> Unit = {},
        onConnected: (MediaController) -> Unit = {},
        onEnded: (MediaController) -> Unit = {},
        onIdle: (MediaController) -> Unit = {},
        onReady: (MediaController) -> Unit = {},
    ): Boolean {
        if (!isServiceConnected()) {
            controlConnectListener.add(onBuffer)
            controlConnectListener.add(onConnected)
            controlConnectListener.add(onEnded)
            controlConnectListener.add(onIdle)
            controlConnectListener.add(onReady)
            return false
        }
        onConnected(mediaController)
        whenIdle(onIdle)
        whenBuffer(onBuffer)
        whenReady(onReady)
        whenEnded(onEnded)
        return true
    }

    private fun isPlayerIdling() = mediaController.playbackState == Player.STATE_IDLE
    private fun isPlayerBuffering() = mediaController.playbackState == Player.STATE_BUFFERING
    private fun isPlayerReady() = mediaController.playbackState == Player.STATE_READY
    private fun isPlayerEnded() = mediaController.playbackState == Player.STATE_ENDED

    @MainThread
    override fun connectService(): Boolean {
        if (isServiceConnected()) {
            controlConnected()
            Timber.d("Service Already Connected, returning...")
            return true
        }
        Timber.d("Service Not Connected, Connecting...")

        sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        futureMediaController = MediaController.Builder(context, sessionToken!!).buildAsync()
        futureMediaController.addListener( {
            mediaController = _mediaController!!
            setupController(mediaController)
            controlConnected()
        }, MoreExecutors.directExecutor())
        return (_mediaController != null)
    }

    private fun setupController(controller: MediaController) {
        with(controller) {
            addListener( object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_IDLE -> controlIdle()
                        Player.STATE_BUFFERING -> controlBuffer()
                        Player.STATE_READY -> controlReady()
                        Player.STATE_ENDED -> controlEnded()
                    }
                }
            })

        }
    }

    fun broadcast(intent: Intent) {
        context.sendBroadcast(intent)
    }

    // Idk if its good approach
    private var controlReadyListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlBufferListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlEndedListener = mutableListOf<( (MediaController) -> Unit )>()
    private var controlIdleListener = mutableListOf<( (MediaController) -> Unit)>()
    private var controlConnectListener = mutableListOf<( (MediaController) -> Unit )>()

    private var locke = Any()
    private fun controlConnected() = synchronized(locke) { controlConnectListener.forEach { it(mediaController) }
        this.controlReadyListener.clear()
    }

    // executed when Player.STATE changed
    private var lock = Any()
    private fun controlReady() = synchronized(lock) { controlReadyListener.forEach { it(mediaController) }
        this.controlReadyListener.clear()
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

    @MainThread
    private fun whenReady( command: (MediaController) -> Unit ) {
        if (this.controlReadyListener.size > 10) this.controlReadyListener.removeAt(0)
        if (mediaController.playbackState == Player.STATE_READY) {
            command(mediaController)
        } else { this.controlReadyListener.add(command) }
    }

    @MainThread
    private fun whenBuffer( command: ( MediaController) -> Unit  ) {
        if (controlBufferListener.size > 10) controlBufferListener.removeAt(0)
        if (mediaController.playbackState == Player.STATE_BUFFERING) {
            command(mediaController)
        } else controlBufferListener.add(command)
    }

    @MainThread
    private fun whenEnded(command: ( (MediaController) -> Unit ) ) {
        if (controlEndedListener.size > 10) controlEndedListener.removeAt(0)
        if (mediaController.playbackState == Player.STATE_ENDED) {
            command(mediaController)
        } else controlEndedListener.add(command)
    }

    @MainThread
    private fun whenIdle( command: ( (MediaController) -> Unit ) ) {
        if (controlIdleListener.size > 10) controlIdleListener.removeAt(0)
        if (mediaController.playbackState == Player.STATE_IDLE) {
            command(mediaController)
        } else this.controlReadyListener.add(command)
    }


    companion object {
        @MainThread
        fun executeOnReady(serviceConnectorImpl: ServiceConnectorImpl, caller: String) {
            Timber.d("ServiceConnector executeOnReady $caller")
            serviceConnectorImpl.controlConnected()
        }
    }
}