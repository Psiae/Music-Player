package com.kylentt.disposed.musicplayer.domain

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.app.settings.AppSettings
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.disposed.musicplayer.domain.mediasession.MediaIntentHandler
import com.kylentt.disposed.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.disposed.musicplayer.domain.mediasession.service.MediaServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@HiltViewModel
internal class MediaViewModel @Inject constructor(
  private val intentHandler: MediaIntentHandler,
  private val savedStateHandle: SavedStateHandle,
  private val sessionManager: MediaSessionManager,
  private val protoRepository: ProtoRepository
) : ViewModel() {

  var pendingNewIntent = mutableListOf<IntentWrapper>()

  val appSettings = protoRepository.appSettingSF

  suspend fun writeAppSettings(data: suspend (AppSettings) -> AppSettings) =
    protoRepository.writeToSettings { data(it) }

  val itemBitmap = mutableStateOf<ItemBitmap>(ItemBitmap.EMPTY)

  val pendingGranted = mutableListOf<() -> Unit>()

  // more explicit
  val serviceState = mutableStateOf<MediaServiceState>(MediaServiceState.UNIT)

  val getConnectedStateHandle: Boolean
    get() = savedStateHandle.get<Boolean>("CONNECTED") ?: false

  fun connectService() {
    sessionManager.connectService()
    savedStateHandle.set("CONNECTED", true)
  }

  suspend fun handleIntent(wrapped: IntentWrapper) = withContext(coroutineContext) {
    intentHandler.handleIntent(wrapped)
  }

  @OptIn(ExperimentalTime::class)
  fun handleMediaIntent(wrapped: IntentWrapper) {
    if (!wrapped.shouldHandleIntent) return
    viewModelScope.launch {
      val (_: kotlin.Unit, time: kotlin.time.Duration) = measureTimedValue { handleIntent(wrapped) }
      Timber.d("MainActivity HandledIntent with ${time.inWholeMilliseconds}ms")
      wrapped.markHandled()
    }
  }

  private suspend fun collectServiceState(): Unit = sessionManager.serviceState.collect { state ->
    when (state) {
      is MediaServiceState.ERROR -> { /* TODO */ }
      else -> serviceState.value = state
    }
  }

  private suspend fun collectItemBitmap(): Unit = sessionManager.bitmapState.collect { bitmap ->
    val item = sessionManager.itemState.first()
    val ibm = itemBitmap.value
    itemBitmap.value = ItemBitmap(
      bitmapDrawable = bitmap, lastBitmapDrawable = ibm.bitmapDrawable, item = item, fade = item != ibm.item
    )
  }

  init {
    viewModelScope.launch {
      collectServiceState()
    }
    viewModelScope.launch {
      collectItemBitmap()
    }
  }
}

data class ItemBitmap(
  val bitmapDrawable: BitmapDrawable?,
  val lastBitmapDrawable: BitmapDrawable?,
  val item: MediaItem,
  val fade: Boolean = true
) {
  companion object {
    val EMPTY = ItemBitmap(null,null, MediaItem.EMPTY)
  }
}
