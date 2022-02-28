package com.kylentt.mediaplayer.domain.presenter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.domain.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    fun connectorToast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

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
    }.conflate().flowOn(Dispatchers.IO)

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

    private var controlReadyListener = mutableListOf<( (MediaController) -> Unit )>()
    private fun clearListener() = controlReadyListener.clear()
    private val lock = Any()
    private fun onReady () = synchronized(lock) {
        controlReadyListener.forEach {
            Timber.d("onReady Executed")
            it(mediaController)
        }
        clearListener()
    }

    // for Dispatchers
    suspend fun sController(f: Boolean = false, command: (MediaController) -> Unit): Boolean
    = withContext(Dispatchers.Main) { controller(f) { command(it) } }

    @MainThread
    fun controller(f: Boolean = false, command: (MediaController) -> Unit): Boolean {
        Timber.d("ServiceConnector Controller")
        return when {
            isServiceConnected() -> {
                Timber.d("ServiceConnector Controller Command")
                command(mediaController)
                true
            }
            f -> {
                Timber.d("ServiceConnector Controller add Command")
                if (controlReadyListener.size > 10) controlReadyListener.removeAt(0)
                controlReadyListener += command
                false
            }
            else -> false
        }
    }

    @MainThread
    override fun connectService(): Boolean {
        if (isServiceConnected()) {
            Timber.d("Service Already Connected, returning...")
            return true
        }
        Timber.d("Service Not Connected, Connecting...")

        sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        futureMediaController = MediaController.Builder(context, sessionToken!!).buildAsync()
        futureMediaController.addListener( {
            mediaController = _mediaController!!
            setupController(mediaController)
            executeOnReady(this, "Future Executor")
        }, MoreExecutors.directExecutor())
        return (_mediaController != null)
    }

    private fun setupController(controller: MediaController) {
        with(controller) {
            addListener( object : Player.Listener {

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.mediaMetadata?.let {
                        _mediaItem.value = mediaItem
                        _playerIndex.value = currentMediaItemIndex
                    }
                }

                override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onPlaylistMetadataChanged(mediaMetadata)
                    val toReturn = mutableListOf<MediaItem>()
                    for (i in 0..mediaItemCount) {
                        toReturn.add(getMediaItemAt(i))
                    }
                    _mediaItems.value = toReturn
                }
            })
        }
    }

    fun broadcast(intent: Intent) {
        context.sendBroadcast(intent)
    }

    var fading = false
    suspend fun exoFade(listener: (MediaController) -> Unit )
    = withContext(Dispatchers.Main) {
        if (!isServiceConnected()) connectService()
        if (fading) { listener(mediaController) ; return@withContext }
        if (!::mediaController.isInitialized) { listener(mediaController) ; return@withContext}
        if (!mediaController.isPlaying) { listener(mediaController) ; return@withContext}

        fading = true
        val exo = mediaController
        while (exo.volume >= 0f) {
            if (exo.volume < 0.1f) {
                listener(exo)
                exo.volume = 1f
                fading = false
                break
            }
            exo.volume = exo.volume -0.1f
            delay(100)
        }
    }

    companion object {
        @MainThread
        fun executeOnReady(serviceConnectorImpl: ServiceConnectorImpl, caller: String) {
            Timber.d("ServiceConnector executeOnReady $caller")
            serviceConnectorImpl.onReady()
        }
    }
}