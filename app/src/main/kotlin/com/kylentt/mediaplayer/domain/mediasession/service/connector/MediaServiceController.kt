package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.annotation.GuardedBy
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.core.exoplayer.Constants
import com.kylentt.mediaplayer.core.exoplayer.Helper
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class MediaServiceController(
  serviceConnector: MediaServiceConnector
) {

  val itemBitmapSF = MutableStateFlow<Pair<MediaItem, BitmapDrawable?>>(Pair(MediaItem.EMPTY, null))
  val playbackStateSF = MutableStateFlow<PlaybackState>(PlaybackState.EMPTY)
  val serviceStateSF = MutableStateFlow<MediaServiceState>(MediaServiceState.NOTHING)

  @GuardedBy("controllerLock")
  lateinit var futureMediaController: ListenableFuture<MediaController>
  lateinit var mediaController: MediaController
  lateinit var sessionToken: SessionToken

  private val controllerLock = Any()
  private val directExecutor = MoreExecutors.directExecutor()

  val context = serviceConnector.baseContext
  val itemHelper = serviceConnector.itemHelper
  val ioScope = serviceConnector.appScope.ioScope
  val mainScope = serviceConnector.appScope.mainScope

  val ioDispatcher = serviceConnector.dispatchers.io
  val mainDispatcher = serviceConnector.dispatchers.main

  val isControllerConnected
    get() = if (::mediaController.isInitialized) mediaController.isConnected else false
  val isControllerConnecting
    get() = serviceStateSF.value is MediaServiceState.CONNECTING

  fun connectController(
    onConnected: (MediaController) -> Unit
  ) = synchronized(controllerLock) {
    verifyMainThread()
    if (isControllerConnected) {
      return onConnected(mediaController)
    }
    if (isControllerConnecting) {
      require(::futureMediaController.isInitialized)
      return futureMediaController.addListener({ onConnected(mediaController) }, directExecutor)
    }
    serviceStateSF.value = MediaServiceState.CONNECTING
    sessionToken = SessionToken(context, MediaService.getComponentName(context))
    futureMediaController =
      MediaController
        .Builder(context, sessionToken)
        .setApplicationLooper(Looper.myLooper()!!)
        .buildAsync()
        .apply {
          addListener(
            {
              try {
                val c = this.get()
                mediaController = c
                setupMediaController(c)
                onConnected(c)
                serviceStateSF.value = MediaServiceState.CONNECTED
              } catch (e: Exception) {
                serviceStateSF.value = MediaServiceState.ERROR("${Timber.e(e)}", e)
              }
            },
            directExecutor
          )
        }
  }

  private fun setupMediaController(controller: MediaController) {
    verifyMainThread()

    with(controller) {

      addListener(object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
          super.onPlaybackStateChanged(playbackState)
          when (playbackState) {
            Player.STATE_BUFFERING -> {
              controlBufferListener.forEachClear(listenerLock) { it(mediaController) }
            }
            Player.STATE_ENDED -> {
              controlEndedListener.forEachClear(listenerLock) { it(mediaController) }
            }
            Player.STATE_IDLE -> {
              controlIdleListener.forEachClear(listenerLock) { it(mediaController) }
            }
            Player.STATE_READY -> {
              controlReadyListener.forEachClear(listenerLock) { it(mediaController) }
            }
          }
          playbackStateSF.value = playbackStateSF.value.copy(playerState = playbackState)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
          super.onPlayWhenReadyChanged(playWhenReady, reason)
          playbackStateSF.value = playbackStateSF.value.copy(playWhenReady = playWhenReady)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          super.onMediaItemTransition(mediaItem, reason)
          val item = mediaItem ?: MediaItem.EMPTY
          playbackStateSF.value = playbackStateSF.value.copy(currentMediaItem = item)
          updateItemBitmapSF(item)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          super.onIsPlayingChanged(isPlaying)
          playbackStateSF.value = playbackStateSF.value.copy(isPlaying = isPlaying)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
          super.onRepeatModeChanged(repeatMode)
          playbackStateSF.value = playbackStateSF.value.copy(repeatMode = repeatMode)
        }
      })
    }
  }

  private val commandLock = Any()
  fun commandController(command: ControllerCommand): Unit = synchronized(commandLock) {
    verifyMainThread()
    if (!isControllerConnected) {
      connectController { commandController(command) }
      return
    }
    with(mediaController) {
      when (command) {
        ControllerCommand.PREPARE -> prepare()
        ControllerCommand.STOP -> stop()
        ControllerCommand.SeekToNext -> seekToNext()
        ControllerCommand.SeekToNextItem -> seekToNextMediaItem()
        ControllerCommand.SeekToPrev -> seekToPrevious()
        ControllerCommand.SeekToPrevItem -> seekToPreviousMediaItem()
        is ControllerCommand.MultiCommand -> {
          command.commands.forEach { commandController(it) }
        }
        is ControllerCommand.SeekToIndex -> {
          seekTo(command.index, command.startPosition)
        }
        is ControllerCommand.SeekToPosition -> {
          seekTo(command.position)
        }
        is ControllerCommand.SetMediaItems -> {
          setMediaItems(command.items, command.startIndex, command.startPosition)
        }
        is ControllerCommand.SetPlayWhenReady -> {
          playWhenReady = command.play
        }
        is ControllerCommand.SetRepeatMode -> {
          repeatMode = command.mode
        }
        is ControllerCommand.WhenReady -> {
          whenReady { command.command }
        }
        is ControllerCommand.WithFadeOut -> {
          mainScope.launch { fadeOut(command) }
        }
      }
    }
  }

  /**
   * All Command Below does Not check for MediaController Initialization
   * assuming the entry point is the commandController function which already check for it
   * */

  val listenerLock: Any = Any()
  val playerVolume: Float
    get() {
      verifyMainThread()
      return mediaController.volume
    }
  val playerIsPlaying: Boolean
    get() {
      verifyMainThread()
      return mediaController.isPlaying
    }

  val maxVolume = Constants.MAX_VOLUME
  val minVolume = Constants.MIN_VOLUME

  @GuardedBy("listenerLock")
  private val controlIdleListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlBufferListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlReadyListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlEndedListener = mutableListOf<(MediaController) -> Unit>()

  private fun whenReady(listener: (MediaController) -> Unit) {
    verifyMainThread()
    if (mediaController.playbackState.isStateReady()) {
      return listener(mediaController)
    }
    controlReadyListener.add(listener)
  }

  private fun whenBuffering(listener: (MediaController) -> Unit) {
    verifyMainThread()
    if (mediaController.playbackState.isStateBuffering()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun whenIdling(listener: (MediaController) -> Unit) {
    verifyMainThread()
    if (mediaController.playbackState.isStateIdle()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun whenEnded(listener: (MediaController) -> Unit) {
    verifyMainThread()
    if (mediaController.playbackState.isStateEnded()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun setPlayerVolume(
    @FloatRange(from = 0.0, to = 1.0) v: Float
  ) {
    verifyMainThread()
    val fv = Helper.fixVolumeToRange(v)
    mediaController.volume = fv
    Timber.d("setPlayerVolume to $v or $fv")
  }

  private val fadeOutLock = Any()

  @GuardedBy("fadeOutLock")
  @Volatile
  private var fadingOut = false

  @GuardedBy("fadeOutLock")
  private val fadeOutListener = mutableListOf<ControllerCommand.WithFadeOut>()

  suspend fun fadeOut(
    command: ControllerCommand.WithFadeOut
  ) {
    verifyMainThread()
    synchronized(fadeOutLock) {
      if (command.flush) fadeOutListener.clear()
      fadeOutListener.add(command)

      if (fadingOut || !playerIsPlaying) {
        fadingOut = false
        fadeOutListener.forEachClear(fadeOutLock) { commandController(it.command) }
        return whenReady { setPlayerVolume(maxVolume) }
      }

      fadingOut = true
    }

    val to = command.to
    val duration = command.duration.toFloat()
    val interval = command.interval.toFloat()
    val step = duration / interval
    val deltaVol = playerVolume / step

    while (playerVolume > to
      && fadingOut
    ) {
      setPlayerVolume(playerVolume - deltaVol)
      delay(command.interval)
    }

    synchronized(fadeOutLock) {
      if (fadingOut) {
        fadingOut = false
        fadeOutListener.forEachClear(fadeOutLock) { commandController(it.command) }
        return whenReady { setPlayerVolume(maxVolume) }
      }
    }
  }

  @Volatile
  var updateItemBitmapJob = Job().job

  fun updateItemBitmapSF(item: MediaItem) {
    updateItemBitmapJob.cancel()
    updateItemBitmapJob = mainScope.launch {
      val bitmap = withContext(ioDispatcher) {
        itemHelper.getEmbeddedPicture(item)
          ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
      }
      val bitmapDrawable = bitmap?.let { BitmapDrawable(context.resources, it) }
      if (playbackStateSF.value.currentMediaItem == item) withContext(mainDispatcher) {
        ensureActive()
        itemBitmapSF.value = Pair(item, bitmapDrawable)
      }
    }
  }
}

sealed class ControllerCommand {
  object PREPARE : ControllerCommand()
  object STOP : ControllerCommand()
  object SeekToNext : ControllerCommand()
  object SeekToPrev : ControllerCommand()
  object SeekToNextItem : ControllerCommand()
  object SeekToPrevItem : ControllerCommand()

  data class MultiCommand(val commands: List<ControllerCommand>) : ControllerCommand()
  data class SeekToPosition(val position: Long) : ControllerCommand()
  data class SetPlayWhenReady(val play: Boolean) : ControllerCommand()
  data class SetRepeatMode(val mode: @Player.RepeatMode Int) : ControllerCommand()
  data class WhenReady(val command: ControllerCommand) : ControllerCommand()

  data class SeekToIndex(
    val index: Int,
    val startPosition: Long
  ) : ControllerCommand()

  data class SetMediaItems(
    val items: List<MediaItem>,
    val startIndex: Int = 0,
    val startPosition: Long = 0L
  ) : ControllerCommand()

  data class WithFadeOut(
    val command: ControllerCommand,
    val flush: Boolean,
    val duration: Long,
    val interval: Long,
    @FloatRange(from = 0.0, to = 1.0) val to: Float = 0f
  ) : ControllerCommand()
}

data class PlaybackState(
  val currentMediaItem: MediaItem,
  val isPlaying: Boolean,
  val playerState: @Player.State Int,
  val playWhenReady: Boolean,
  val repeatMode: @Player.RepeatMode Int
) {
  companion object {
    val EMPTY = PlaybackState(
      isPlaying = false,
      playWhenReady = false,
      currentMediaItem = MediaItem.EMPTY,
      playerState = Player.STATE_IDLE,
      repeatMode = Player.REPEAT_MODE_OFF
    )
  }
}

sealed class MediaServiceState {
  object NOTHING : MediaServiceState()
  object CONNECTING : MediaServiceState()
  object CONNECTED : MediaServiceState()
  object DISCONNECTED : MediaServiceState()

  data class ERROR(val msg: String, val exception: Exception) : MediaServiceState()
}
