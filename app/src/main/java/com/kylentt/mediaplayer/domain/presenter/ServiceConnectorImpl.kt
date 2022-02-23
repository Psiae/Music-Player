package com.kylentt.mediaplayer.domain.presenter

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.domain.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Application Context
class ServiceConnectorImpl(
    val context: Context
) : ServiceConnector {

    // TODO: Find usage of MediaBrowser

    private val _isPlaying = MutableStateFlow<Boolean?>(null)
    val isPlaying = _isPlaying.asStateFlow()
    private val _playerIndex = MutableStateFlow<Int?>(null)
    val playerIndex = _playerIndex.asStateFlow()
    private val _mediaItem = MutableStateFlow<MediaItem?>(null)
    val mediaItem = _mediaItem.asStateFlow()
    private val _mediaItems = MutableStateFlow<List<MediaItem>?>(null)
    val mediaItems = _mediaItems.asStateFlow()

    lateinit var mediaController: MediaController

    fun controller(command: (MediaController) -> Boolean ): Boolean {
        return if (::mediaController.isInitialized) {
            command(mediaController)
        } else false
    }

    // Media
    private lateinit var sessionToken: SessionToken
    private lateinit var futureMediaController: ListenableFuture<MediaController>
    private val _mediaController: MediaController?
        get() = if (futureMediaController.isDone) futureMediaController.get() else null

    override fun isServiceConnected(): Boolean {
        return if (this::futureMediaController.isInitialized) {
            _mediaController?.isConnected == true
        } else false
    }

    override fun connectService() {
        sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        futureMediaController = MediaController.Builder(context, sessionToken).buildAsync()

        _mediaController?.let {
            mediaController = it
        } ?: futureMediaController.addListener(
            { mediaController = _mediaController!! ; setupController(mediaController) },
            MoreExecutors.directExecutor()
        )
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