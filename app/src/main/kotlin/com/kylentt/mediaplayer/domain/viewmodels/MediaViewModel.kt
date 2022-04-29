package com.kylentt.mediaplayer.domain.viewmodels

import android.graphics.drawable.BitmapDrawable
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
  private val mediaSessionManager: MediaSessionManager
) : ViewModel() {

  val mediaItemBitmap = MutableStateFlow(MediaItemBitmap.EMPTY)

  @MainThread
  fun connectService() {
    verifyMainThread()
    mediaSessionManager.connectService()
  }

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch { mediaSessionManager.handleMediaIntent(intent) }
  }

  init {
    viewModelScope.launch {
      mediaSessionManager.itemBitmap.collect {
        mediaItemBitmap.value = MediaItemBitmap(it.first, it.second)
      }
    }
  }

}

data class MediaItemBitmap(
  val item: MediaItem,
  val bitmapDrawable: BitmapDrawable?
) {
  companion object {
    val EMPTY = MediaItemBitmap(MediaItem.EMPTY, null)
  }
}
