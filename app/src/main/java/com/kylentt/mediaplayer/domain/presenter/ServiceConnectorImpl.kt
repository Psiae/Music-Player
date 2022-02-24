package com.kylentt.mediaplayer.domain.presenter

import android.content.ComponentName
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.domain.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

// Application Context
class ServiceConnectorImpl(
    val context: Context
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

    fun getPos() = mediaController.currentPosition

    fun getDur() = mediaController.duration


    // Media
    private var sessionToken: SessionToken? = null
    private lateinit var futureMediaController: ListenableFuture<MediaController>
    private lateinit var mediaController: MediaController
    private val _mediaController: MediaController?
        get() = if (futureMediaController.isDone) futureMediaController.get() else null

    override fun isServiceConnected(): Boolean {
        if (!::mediaController.isInitialized) return false
        if (sessionToken == null) return false
        if (mediaController.connectedToken == null) return false
        return (mediaController.isConnected)
    }

    fun releaseSession() {
        controlReady = false
        sessionToken = null
        mediaController.release()
    }

    private var controlReadyListener = mutableListOf<( (MediaController) -> Unit )>()

    private fun clearListener() = controlReadyListener.clear()

    private var controlReady = false
        set(value) {
            if (value && !field) {
                synchronized(this) {
                    controlReadyListener.forEach { it(mediaController) }
                    clearListener()
                    field = value
                }
            } else field = value
        }

    var retry = true

    @MainThread
    fun controller(f: Boolean = true, command: (MediaController) -> Unit): Boolean {
        Timber.d("Controller $sessionToken")
        return when {
            sessionToken == null -> {
                connectService()
                return if (retry) {
                    retry = false
                    controller(command = command)
                } else false
            }
            ::mediaController.isInitialized -> {
                command(mediaController)
                true
            }
            f -> {
                if (controlReadyListener.size > 10) controlReadyListener.removeAt(0)
                controlReadyListener += command
                true
            }
            else -> false
        }
    }

    @MainThread
    override fun connectService() {
        if (isServiceConnected()) return
        Timber.d("Service Not Connected")
        sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        futureMediaController = MediaController.Builder(context, sessionToken!!).buildAsync()
        futureMediaController.addListener( {
            mediaController = _mediaController!!
            setupController(mediaController)
            controlReady = true
        }, MoreExecutors.directExecutor())
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
}