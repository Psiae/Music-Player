package com.kylentt.mediaplayer.domain.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.connector.PlaybackState
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class MediaViewModel @Inject constructor(
  private val dispatchers: AppDispatchers,
  private val itemHelper: MediaItemHelper,
  private val mediaSessionManager: MediaSessionManager
) : ViewModel() {

  val mediaItemBitmap = MutableStateFlow(MediaItemBitmap.EMPTY)
  val mediaPlaybackState = MutableStateFlow(PlaybackState.EMPTY)

  private var updateItemBitmapJob = Job().job

  @MainThread
  fun connectService() {
    checkMainThread()
    mediaSessionManager.connectService()
  }

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch { mediaSessionManager.handleMediaIntent(intent) }
  }

  private suspend fun collectPlaybackState() {
    mediaSessionManager.playbackState.collect { playbackState ->
      mediaPlaybackState.value = playbackState
      if (playbackState.currentMediaItem != mediaItemBitmap.value.item) {
        dispatchUpdateItemBitmap(playbackState.currentMediaItem)
      }
    }
  }

  private suspend fun dispatchUpdateItemBitmap(item: MediaItem) {
    updateItemBitmapJob.cancel()
    updateItemBitmapJob = viewModelScope.launch {
      withContext(dispatchers.io) {
        val bitmap = itemHelper.getEmbeddedPicture(item)?.let { bytes: ByteArray ->
          ensureActive()
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        withContext(dispatchers.main) {
          if (!isActive) bitmap?.recycle()
          ensureValidCurrentItem(item) { "dispatchUpdateItemBitmap inconsistent" }
          mediaItemBitmap.value = MediaItemBitmap(item, bitmap)
        }
      }
    }
  }

  @MainThread
  private suspend inline fun ensureValidCurrentItem(item: MediaItem, msg: () -> Any) {
    checkMainThread()
    coroutineContext.ensureActive()
    val current = mediaPlaybackState.value.currentMediaItem
    checkState(item == current) {
      "checkValidCurrentItem failed." +
        "\nexpected : ${item.mediaMetadata.displayTitle}, id: ${item.mediaId}" +
        "\ncurrent: $current, id: ${item.mediaId}" +
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
