package com.kylentt.mediaplayer.domain.viewmodels

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.core.app.coroutines.safeCollect
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemHelper
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.external.MediaIntentHandler
import com.kylentt.musicplayer.common.context.DeviceInfo
import com.kylentt.musicplayer.common.extenstions.checkCancellation
import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState
import com.kylentt.musicplayer.ui.util.BitmapFactoryHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@HiltViewModel
class MediaViewModel @Inject constructor(
		private val deviceInfo: DeviceInfo,
    private val dispatchers: CoroutineDispatchers,
    private val itemHelper: MediaItemHelper,
    private val intentHandler: MediaIntentHandler
) : ViewModel() {

  private val ioScope = viewModelScope + dispatchers.io
	private val computationScope = viewModelScope + dispatchers.computation

  val mediaItemBitmap = MutableStateFlow(MediaItemBitmap.EMPTY)
  val mediaPlaybackState = MutableStateFlow(PlaybackState.EMPTY)

  private var updateItemBitmapJob = Job().job

  @MainThread
  fun connectService() = Unit

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch(dispatchers.computation) {
			if (intent.shouldHandleIntent) intentHandler.handleMediaIntent(intent)
		}
  }

  private suspend fun collectPlaybackState() {
    Timber.d("MediaViewModel collectPlaybackState")
    MusicLibrary.localAgent.session.info.playbackState.safeCollect { playbackState ->
      Timber.d("MediaViewModel collectPlaybackState collected for: " +
        "\n${playbackState.mediaItem.getDebugDescription()}"
      )
			val get = mediaPlaybackState.value
      mediaPlaybackState.value = playbackState
      if (playbackState.mediaItem !== get.mediaItem) {
        dispatchUpdateItemBitmap(playbackState.mediaItem)
      }
    }
  }

  @OptIn(ExperimentalTime::class)
	@MainThread
  private suspend fun dispatchUpdateItemBitmap(item: MediaItem) {
    updateItemBitmapJob.cancel()
    updateItemBitmapJob = computationScope.launch {
      Timber.d("UpdateItemBitmap Dispatched For ${item.getDebugDescription()}")

			val measureGet = measureTimedValue {
				itemHelper.getEmbeddedPicture(item)
			}

			Timber.d("getEmbeddedPicture took ${measureGet.duration.inWholeMilliseconds}ms")

			ensureActive()

			val width = deviceInfo.screenWidthPixel
			val height = deviceInfo.screenHeightPixel

      val measureDecode = measureTimedValue {
				measureGet.value?.let { bytes: ByteArray ->
					BitmapFactoryHelper.decodeByteArrayToSampledBitmap(bytes,
						0, bytes.size, width, height,
						BitmapFactoryHelper.SubSampleCalculationType.MaxAlloc( /* 10MB */10000000)
					)
				}
			}

			Timber.d("decodeByteArray with size: ${measureGet.value?.size} " +
				"took ${measureDecode.duration.inWholeMilliseconds}ms" +
				"\nrequestedSize: $width:$height" +
				"\nsize: ${measureDecode.value?.width}:${measureDecode.value?.height}" +
				"\nalloc: ${measureDecode.value?.allocationByteCount ?: "0 / null"}")

			val bitmap = measureDecode.value
			ensureActive()

			checkCancellation {
				bitmap?.recycle()
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
    val current = mediaPlaybackState.value.mediaItem
    checkState(item === current) {
      "checkValidCurrentItem failed." +
        "\nexpected : ${item.getDebugDescription()}" +
        "\ncurrent: ${current.getDebugDescription()}" +
        "\nmsg: ${msg()}"
    }
  }

  init {
    viewModelScope.launch(dispatchers.main) {
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
