package com.kylentt.mediaplayer.domain.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemHelper
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.connector.PlaybackState
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val dispatchers: AppDispatchers,
    private val itemHelper: MediaItemHelper,
    private val mediaSessionConnector: MediaSessionConnector
) : ViewModel() {

  private val ioScope = viewModelScope + dispatchers.io

  val mediaItemBitmap = MutableStateFlow(MediaItemBitmap.EMPTY)
  val mediaPlaybackState = MutableStateFlow(PlaybackState.EMPTY)

  private var updateItemBitmapJob = Job().job

  @MainThread
  fun connectService() {
    checkMainThread()
    mediaSessionConnector.connectService()
  }

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch(dispatchers.computation) { mediaSessionConnector.handleMediaIntent(intent) }
  }

  private suspend fun collectPlaybackState() {
    Timber.d("MediaViewModel collectPlaybackState")
    mediaSessionConnector.playbackState.collect { playbackState ->
      Timber.d("MediaViewModel collectPlaybackState collected for: " +
        "\n${playbackState.currentMediaItem.getDebugDescription()}"
      )
      mediaPlaybackState.value = playbackState
      if (playbackState.currentMediaItem !== mediaItemBitmap.value.item) {
        dispatchUpdateItemBitmap(playbackState.currentMediaItem)
      }
    }
  }

  @MainThread
  private suspend fun dispatchUpdateItemBitmap(item: MediaItem) {
    updateItemBitmapJob.cancel()
    updateItemBitmapJob = ioScope.launch {
      Timber.d("UpdateItemBitmap Dispatched For ${item.getDebugDescription()}")
      val bitmap = itemHelper.getEmbeddedPicture(item)?.let { bytes: ByteArray ->
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      }
      withContext(dispatchers.main) {
        if (!isActive) bitmap?.recycle()
        ensureValidCurrentItem(item) { "dispatchUpdateItemBitmap inconsistent" }
        mediaItemBitmap.value = MediaItemBitmap(item, bitmap)
        Timber.d("UpdateItemBitmap Success For ${item.getDebugDescription()}")
      }
    }
  }

  @MainThread
  private suspend inline fun ensureValidCurrentItem(item: MediaItem, msg: () -> Any) {
    checkMainThread()
    coroutineContext.ensureActive()
    val current = mediaPlaybackState.value.currentMediaItem
    checkState(item === current) {
      "checkValidCurrentItem failed." +
        "\nexpected : ${item.getDebugDescription()}" +
        "\ncurrent: ${current.getDebugDescription()}" +
        "\nmsg: ${msg()}"
    }
  }

  init {
    viewModelScope.launch {
      collectPlaybackState()
    }
  }

}

data class MediaItemBitmap(
  val item: MediaItem,
  val bitmap: Bitmap?
) {
  companion object {
    val EMPTY = MediaItemBitmap(MediaItem.EMPTY, null)
  }
}
