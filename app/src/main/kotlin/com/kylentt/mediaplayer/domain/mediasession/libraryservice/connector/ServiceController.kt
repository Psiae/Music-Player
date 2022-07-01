package com.kylentt.mediaplayer.domain.mediasession.libraryservice.connector

import android.os.Bundle
import android.os.Looper
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.core.exoplayer.PlayerHelper
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Class that handle [ControllerCommand] to control MediaPlayback using [MediaController]
 * [MediaService] is started from [connectController] Function
 * @throws [IllegalStateException] if accessed from other than [Looper.getMainLooper
 * @author Kylentt
 * @since 2022/04/30
 */

@MainThread
class MediaServiceController(
  serviceConnector: ServiceConnector
) {

  val playbackStateSF = MutableStateFlow<PlaybackState>(PlaybackState.EMPTY)
  val serviceStateSF = MutableStateFlow<MediaServiceState>(MediaServiceState.NOTHING)

  private lateinit var futureMediaController: ListenableFuture<MediaController>
  private lateinit var mediaController: MediaController
  private lateinit var sessionToken: SessionToken

  private val context = serviceConnector.baseContext
  private val directExecutor = MoreExecutors.directExecutor()

  private val mainScope = serviceConnector.appScope.mainScope
  private val mainDispatcher = serviceConnector.dispatchers.main
  private val mainImmediateDispatcher = serviceConnector.dispatchers.mainImmediate

  private val controlBufferListener = mutableListOf<(MediaController) -> Unit>()
  private val controlEndedListener = mutableListOf<(MediaController) -> Unit>()
  private val controlIdleListener = mutableListOf<(MediaController) -> Unit>()
  private val controlReadyListener = mutableListOf<(MediaController) -> Unit>()

  private val isControllerConnected
    get() = elseFalse(::mediaController.isInitialized) { mediaController.isConnected }

  private val isControllerConnecting
    get() = serviceStateSF.value is MediaServiceState.CONNECTING

  private inline fun elseFalse(condition: Boolean, block: () -> Boolean): Boolean {
    return if (condition) block() else false
  }

  private val playerListener = object : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
      super.onPlaybackStateChanged(playbackState)
      when (playbackState) {
        Player.STATE_BUFFERING -> controlBufferListener.forEachClear { it(mediaController) }
        Player.STATE_ENDED -> controlEndedListener.forEachClear { it(mediaController) }
        Player.STATE_IDLE -> controlIdleListener.forEachClear { it(mediaController) }
        Player.STATE_READY -> controlReadyListener.forEachClear { it(mediaController) }
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
			// not sure whether to take the mediaItem that has LocalConfiguration or Not

			if (!playbackStateSF.value.currentMediaItem.idEqual(mediaItem)) {
				playbackStateSF.value = playbackStateSF.value.copy(currentMediaItem = item)
			}
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

  private val futureControllerListener = {
    with(futureMediaController) {
      try {
        if (!this.isDone) {
          val msg = "FutureControllerListener callback is called before its Done"
          throw IllegalStateException(msg)
        }
        mediaController = this.get()
        setupMediaController()
      } catch (e: Exception) {
        Timber.e(e)
        serviceStateSF.value = MediaServiceState.ERROR("Error Controller Setup", e)
      }
    }
  }

  @MainThread
  fun connectController(
    onConnected: (MediaController) -> Unit
  ): Unit {
    checkMainThread()
    if (isControllerConnected) {
      return onConnected(mediaController)
    }
    if (isControllerConnecting) {
      check(::futureMediaController.isInitialized)
      return futureMediaController.addListener( { onConnected(mediaController) }, directExecutor)
    }
    serviceStateSF.value = MediaServiceState.CONNECTING
    sessionToken = SessionToken(context, MusicLibraryService.getComponentName())
    futureMediaController = MediaController.Builder(context, sessionToken)
			.setConnectionHints( /* later */ Bundle.EMPTY)
      .setApplicationLooper(Looper.myLooper()!!)
      .buildAsync().apply { addListener(futureControllerListener, directExecutor) }
  }

	@MainThread
	fun disconnectController() {
		checkMainThread()

		when {
			isControllerConnected -> mediaController.release()
			isControllerConnecting -> connectController { mediaController.release() }
		}
	}

  fun commandController(command: ControllerCommand): Unit {
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

  private val maxVolume: Float = PlayerConstants.MAX_VOLUME
  private val minVolume: Float = PlayerConstants.MIN_VOLUME

  private val playerVolume: Float
    get() = mediaController.volume

  private val playerIsPlaying: Boolean
    get() = mediaController.isPlaying

	private val playerMediaItem
		get() = mediaController.currentMediaItem

	private val playerPlayWhenReady
		get() = mediaController.playWhenReady

  private val fadeOutListener = mutableListOf<ControllerCommand.WithFadeOut>()

  private fun setupMediaController() {
    checkState(::mediaController.isInitialized) {
      "setupMediaController failed, MediaController has Not been Initialized"
    }
    val controller = this.mediaController
    controller.addListener(playerListener)
    serviceStateSF.value = MediaServiceState.CONNECTED
  }

  private fun releaseMediaController() {
    val controller = this.mediaController
    if (!controller.isConnected) return
    controller.release()
    checkState(!controller.isConnected)
    serviceStateSF.value = MediaServiceState.DISCONNECTED
  }

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

  private var shouldFadeOut: Boolean = true

  private suspend fun fadeOut(
    command: ControllerCommand.WithFadeOut
  ) {

		val pwr = playerPlayWhenReady
		val item = playerMediaItem

		val shouldLoop = { shouldFadeOut && playerIsPlaying && playerVolume > command.to }
		val shouldSkip = { playerPlayWhenReady != pwr || !item.idEqual(playerMediaItem) }
		val whenReady = {
			whenReady { setPlayerVolume(maxVolume) }
			shouldFadeOut = true
		}

    if (command.flush) fadeOutListener.clear()
    fadeOutListener.add(command)

    val to = command.to
    val duration = command.duration.toFloat()
    val interval = command.interval.toFloat()
    val step = duration / interval
    val deltaVol = playerVolume / step

		while (shouldLoop()) {

			if (shouldSkip()) return whenReady()

			setPlayerVolume(playerVolume - deltaVol)
      delay(command.interval)
    }

		fadeOutListener.forEachClear {
			commandController(it.command)
			it.afterFadeOut()
		}
		whenReady()
  }

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return idEqual(that?.mediaId)
  }

  private fun MediaItem?.idEqual(that: String?): Boolean {
    return this?.mediaId == that
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

    @JvmStatic
    val EMPTY by lazy {
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
