package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.app.delegates.LockMainThread
import com.kylentt.mediaplayer.app.delegates.Synchronize
import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants
import com.kylentt.mediaplayer.core.exoplayer.PlayerHelper
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

/**
 * Class that handle [ControllerCommand] to control MediaPlayback using [MediaController]
 * [MediaService] is started from [connectController] Function
 * @throws [IllegalStateException] if accessed from other than [Looper.getMainLooper
 * @author Kylentt
 * @since 2022/04/30
 */

class MediaServiceController(
  serviceConnector: MediaServiceConnector
) {

  val playbackStateSF = MutableStateFlow<PlaybackState>(PlaybackState.EMPTY)
  val serviceStateSF = MutableStateFlow<MediaServiceState>(MediaServiceState.NOTHING)

  @Volatile
  @GuardedBy("controllerLock")
  private lateinit var futureMediaController: ListenableFuture<MediaController>
  private lateinit var mediaController: MediaController
  private lateinit var sessionToken: SessionToken

  private val controllerLock = Any()
  private val listenerLock: Any = Any()
  private val directExecutor = MoreExecutors.directExecutor()

  private val context = serviceConnector.baseContext

  private val mainScope = serviceConnector.appScope.mainScope
  private val mainDispatcher = serviceConnector.dispatchers.main
  private val mainImmediateDispatcher = serviceConnector.dispatchers.mainImmediate

  @GuardedBy("listenerLock")
  private val controlIdleListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlBufferListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlReadyListener = mutableListOf<(MediaController) -> Unit>()

  @GuardedBy("listenerLock")
  private val controlEndedListener = mutableListOf<(MediaController) -> Unit>()

  private val isControllerConnected
    get() = if (::mediaController.isInitialized) mediaController.isConnected else false
  private val isControllerConnecting
    get() = serviceStateSF.value is MediaServiceState.CONNECTING

  private val playerListener = object : Player.Listener {

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
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
      super.onIsPlayingChanged(isPlaying)
      playbackStateSF.value = playbackStateSF.value.copy(isPlaying = isPlaying)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
      super.onRepeatModeChanged(repeatMode)
      playbackStateSF.value = playbackStateSF.value.copy(repeatMode = repeatMode)
    }
  }

  @MainThread
  fun connectController(
    onConnected: (MediaController) -> Unit
  ) = synchronized(controllerLock) {
    checkMainThread()
    if (isControllerConnected) {
      return onConnected(mediaController)
    }
    if (isControllerConnecting) {
      check(::futureMediaController.isInitialized)
      return futureMediaController.addListener({ onConnected(mediaController) }, directExecutor)
    }
    serviceStateSF.value = MediaServiceState.CONNECTING
    sessionToken = SessionToken(context, MediaService.getComponentName(context))
    futureMediaController = MediaController.Builder(context, sessionToken)
      .setApplicationLooper(Looper.myLooper()!!)
      .buildAsync()
      .apply {
        addListener(
          {
            try {
              checkState(this.isDone)
              mediaController = this.get()
              setupMediaController(mediaController)
              onConnected(mediaController)
              serviceStateSF.value = MediaServiceState.CONNECTED
            } catch (e: Exception) {
              serviceStateSF.value = MediaServiceState.ERROR("${Timber.e(e)}", e)
            }
          }, directExecutor
        )
      }
  }

  private fun setupMediaController(controller: MediaController) {
    checkMainThread()
    controller.addListener(playerListener)
  }

  private val commandLock = Any()
  fun commandController(command: ControllerCommand): Unit = synchronized(commandLock) {
    checkMainThread()
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
   * All Command Below does Not check for MediaController Initialization nor Looper
   * assuming the entry point is the commandController function which already check for it
   */

  private val fadeOutLock: Any = Any()
  private val maxVolume: Float = PlayerConstants.MAX_VOLUME
  private val minVolume: Float = PlayerConstants.MIN_VOLUME

  @GuardedBy("fadeOutLock")
  private val fadeOutListener = mutableListOf<ControllerCommand.WithFadeOut>()

  private val playerVolume: Float
    get() = mediaController.volume

  private val playerIsPlaying: Boolean
    get() = mediaController.isPlaying

  private fun whenReady(listener: (MediaController) -> Unit) {
    if (mediaController.playbackState.isStateReady()) {
      return listener(mediaController)
    }
    controlReadyListener.add(listener)
  }

  private fun whenBuffering(listener: (MediaController) -> Unit) {
    if (mediaController.playbackState.isStateBuffering()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun whenIdling(listener: (MediaController) -> Unit) {
    if (mediaController.playbackState.isStateIdle()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun whenEnded(listener: (MediaController) -> Unit) {
    if (mediaController.playbackState.isStateEnded()) {
      return listener(mediaController)
    }
    controlBufferListener.add(listener)
  }

  private fun setPlayerVolume(
    @FloatRange(from = 0.0, to = 1.0) v: Float
  ) {
    val fv = PlayerHelper.fixVolumeToRange(v)
    mediaController.volume = fv
    Timber.d("setPlayerVolume to $v or $fv")
  }

  private var fadingOut by LockMainThread(false)

  private suspend fun fadeOut(
    command: ControllerCommand.WithFadeOut
  ) {

    synchronized(fadeOutLock) {
      if (command.flush) fadeOutListener.clear()
      fadeOutListener.add(command)

      if (fadingOut || !playerIsPlaying || playerVolume == minVolume) {
        fadingOut = false
        fadeOutListener.forEachClear(fadeOutLock) {
          commandController(it.command)
          it.afterFadeOut()
        }
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
        fadeOutListener.forEachClear(fadeOutLock) {
          commandController(it.command)
          it.afterFadeOut()
        }
        return whenReady { setPlayerVolume(maxVolume) }
      }
    }
  }

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return that != null && idEqual(that.mediaId)
  }

  private fun MediaItem?.idEqual(that: String): Boolean {
    return this != null && this.mediaId == that
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
    val command: ControllerCommand, // Expose the cause
    val flush: Boolean,
    val duration: Long,
    val interval: Long,
    @FloatRange(from = 0.0, to = 1.0) val to: Float = 0f,
    val afterFadeOut: () -> Unit = {}
  ) : ControllerCommand()


  companion object {

    @JvmStatic fun ControllerCommand.wrapWithFadeOut(
      flush: Boolean = false,
      duration: Long = 1000L,
      interval: Long = 50L,
      @FloatRange(from = 0.0, to = 1.0) to: Float = 0F,
      afterFadeOut: () -> Unit = {}
    ) = wrapFadeOut(this, flush, duration, interval, to, afterFadeOut)

    @JvmStatic fun wrapFadeOut(
      command: ControllerCommand,
      flush: Boolean = false,
      duration: Long = 1000L,
      interval: Long = 50L,
      to: Float = 0F,
      afterFadeOut: () -> Unit = {}
    ) = WithFadeOut(command, flush, duration, interval, to, afterFadeOut)
  }
}

data class PlaybackState(
  val currentMediaItem: MediaItem,
  val isPlaying: Boolean,
  val playerState: @Player.State Int,
  val playWhenReady: Boolean,
  val repeatMode: @Player.RepeatMode Int
) {
  companion object {
    @JvmStatic val EMPTY by lazy {
      PlaybackState(
        isPlaying = false,
        playWhenReady = false,
        currentMediaItem = MediaItem.EMPTY,
        playerState = Player.STATE_IDLE,
        repeatMode = Player.REPEAT_MODE_OFF
      )
    }
  }
}

sealed class MediaServiceState {
  object NOTHING : MediaServiceState()
  object CONNECTING : MediaServiceState()
  object CONNECTED : MediaServiceState()
  object DISCONNECTED : MediaServiceState()

  data class ERROR(val msg: String, val exception: Exception) : MediaServiceState()
}
