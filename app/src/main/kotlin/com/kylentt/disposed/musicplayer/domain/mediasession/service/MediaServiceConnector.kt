package com.kylentt.disposed.musicplayer.domain.mediasession.service

import android.content.ComponentName
import android.content.Context
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
import coil.Coil
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.core.exoplayer.Constants.MAX_VOLUME
import com.kylentt.mediaplayer.core.exoplayer.Helper.fixVolumeToRange
import com.kylentt.disposed.disposed.core.exoplayer.util.toStrState
import com.kylentt.disposed.disposed.core.util.handler.CoilHandler
import com.kylentt.disposed.disposed.core.util.handler.MediaItemHandler
import com.kylentt.disposed.disposed.domain.mediaSession.service.MusicService
import com.kylentt.disposed.musicplayer.core.helper.MediaItemUtil
import com.kylentt.disposed.musicplayer.core.helper.elseNull
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import com.kylentt.disposed.musicplayer.domain.mediasession.MediaSessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber


sealed class MediaServiceState {
  object UNIT : MediaServiceState()
  object DISCONNECTED : MediaServiceState()
  object CONNECTING : MediaServiceState()
  object CONNECTED : MediaServiceState()
  data class ERROR(val msg: String, val e: Exception) : MediaServiceState()

  companion object {
    fun isConnectAttempted(state: MediaServiceState) = state !is UNIT
  }
}

sealed class PlaybackState {

  abstract fun toStrState(): String

  object UNIT : PlaybackState() {
    override fun toStrState(): String = "UNIT"
  }

  object BUFFERING : PlaybackState() {
    override fun toStrState(): String = Player.STATE_BUFFERING.toStrState()
  }

  object READY : PlaybackState() {
    override fun toStrState(): String = Player.STATE_READY.toStrState()
  }

  object IDLE : PlaybackState() {
    override fun toStrState(): String = Player.STATE_IDLE.toStrState()
  }

  object ENDED : PlaybackState() {
    override fun toStrState(): String = Player.STATE_ENDED.toStrState()
  }

}

sealed class ControllerCommand {

  object Unit : ControllerCommand()
  object Prepare : ControllerCommand()
  object Stop : ControllerCommand()
  data class MultiCommand(val commands: List<ControllerCommand>) : ControllerCommand()
  data class SeekToPos(val pos: Long) : ControllerCommand()
  data class SeekToPercent(val percent: Float) : ControllerCommand()
  data class SetMediaItem(val item: MediaItem) : ControllerCommand()
  data class SetPlayWhenReady(val play: Boolean) : ControllerCommand()
  data class WhenReady(val command: ControllerCommand) : ControllerCommand()

  data class SeekToIndex(
    val index: Int,
    val pos: Long = 0
  ) : ControllerCommand()

  data class SetMediaItems(
    val items: List<MediaItem>,
    val startIndex: Int = 0,
    val startPos: Long = 0L
  ) : ControllerCommand()

  data class WithFade(
    val command: ControllerCommand,
    @FloatRange(from = 0.0, to = 1.0) val to: Float = 0f,
    val interval: Long = 50,
    val duration: Long = 1000,
    val flush: Boolean = false
  ) : ControllerCommand()
}

internal class MediaServiceConnector(
  private val manager: MediaSessionManager,
  private val context: Context,
  appScope: AppScope
) {

  var mediaServiceController = MediaServiceController(
    MediaItemHandler(context),
    CoilHandler(context, Coil.imageLoader(context))
  )
    private set

  private val computeScope = appScope.computationScope
  private val ioScope = appScope.ioScope
  private val mainScope = appScope.mainScope

  private val connectLock = Any()

  private val _playerBitmap = MutableStateFlow<BitmapDrawable?>(null)
  val playerBitmap = _playerBitmap.asStateFlow()

  private val _playerState = MutableStateFlow<PlaybackState>(PlaybackState.UNIT)
  val playerState = _playerState.asStateFlow()

  private val _playerMediaItem = MutableStateFlow<MediaItem>(MediaItem.EMPTY)
  val playerMediaItem = _playerMediaItem.asStateFlow()

  private val _serviceState = MutableStateFlow<MediaServiceState>(MediaServiceState.UNIT)
  val serviceState = _serviceState.asStateFlow()

  private lateinit var sessionToken: SessionToken
  private lateinit var futureMediaController: ListenableFuture<MediaController>
  private lateinit var mediaController: MediaController

  @MainThread
  private fun connectService(
    onConnected: (MediaController) -> Unit
  ) = synchronized(connectLock) {
    verifyMainThread()

    if (isControllerConnected()) {
      onConnected(mediaController)
      return
    }

    if (serviceState.value is MediaServiceState.CONNECTING) {
      futureMediaController.addListener(
        { onConnected(mediaController) },
        MoreExecutors.directExecutor()
      )
      return
    }

    _serviceState.value = MediaServiceState.CONNECTING

    sessionToken = SessionToken(
      context.applicationContext,
      ComponentName(
        context.applicationContext,
        MusicService::class.java
      )
    )
    futureMediaController = MediaController.Builder(context, sessionToken)
      .setApplicationLooper(Looper.getMainLooper())
      .buildAsync()
      .apply {
        addListener({
          with(this.get()) {
            mediaController = this
            mediaServiceController.provideController(mediaController)
            onConnected(mediaController)
            _serviceState.value = MediaServiceState.CONNECTED
          }
        }, MoreExecutors.directExecutor())
      }
  }

  private fun isControllerConnected(): Boolean {
    verifyMainThread()
    return when {
      !::mediaController.isInitialized -> false
      else -> mediaController.isConnected
    }
  }

  inner class MediaServiceController(
    private val itemHandler: MediaItemHandler,
    private val coilHandler: CoilHandler
  ) {

    private lateinit var controller: MediaController

    private val commandLock = Any()
    private val controlListenerLock = Any()

    fun provideController(controller: MediaController) {
      this.controller = controller
      setupController(controller)
    }

    fun commandController(command: ControllerCommand): Unit = synchronized(commandLock) {
      Timber.d("commandController $command")

      verifyMainThread()
      if (!::controller.isInitialized) {
        connectService { commandController(command) }
        return
      }
      if (!controller.isConnected) {
        connectService { commandController(command) }
        return
      }
      when (command) {
        ControllerCommand.Prepare -> {
          mediaController.prepare()
        }
        ControllerCommand.Stop -> {
          mediaController.stop()
        }
        is ControllerCommand.MultiCommand -> {
          command.commands.forEach { commandController(it) }
        }
        is ControllerCommand.SeekToIndex -> {
          mediaController.seekTo(command.index, command.pos)
        }
        is ControllerCommand.SeekToPercent -> {
          mediaController.seekTo(((mediaController.currentPosition.toFloat() / 100) / command.percent).toLong())
        }
        is ControllerCommand.SeekToPos -> {
          mediaController.seekTo(command.pos)
        }
        is ControllerCommand.SetMediaItem -> {
          mediaController.setMediaItem(command.item)
        }
        is ControllerCommand.SetMediaItems -> {
          mediaController.setMediaItems(command.items, command.startIndex, command.startPos)
        }
        is ControllerCommand.SetPlayWhenReady -> {
          mediaController.playWhenReady = command.play
        }
        is ControllerCommand.WhenReady -> {
          if (command.command is ControllerCommand.WhenReady) {
            Timber.e("Invalid Command $command ${command.command}")
            return
          }
          whenReady { commandController(command.command) }
        }
        is ControllerCommand.WithFade -> {
          mainScope.launch { fadeOut(command) }
        }
        ControllerCommand.Unit -> {
          Unit
        }
      }
    }

    private val controlIdleListener = mutableListOf<(MediaController) -> Unit>()
    private val controlBufferListener = mutableListOf<(MediaController) -> Unit>()
    private val controlReadyListener = mutableListOf<(MediaController) -> Unit>()
    private val controlEndedListener = mutableListOf<(MediaController) -> Unit>()

    private fun whenReady(listener: (MediaController) -> Unit) {
      if (mediaController.playbackState == Player.STATE_READY) {
        return listener(mediaController)
      }
      controlReadyListener.add(listener)
    }

    private fun whenBuffer(listener: (MediaController) -> Unit) {
      if (mediaController.playbackState == Player.STATE_READY) {
        return listener(mediaController)
      }
      controlReadyListener.add(listener)
    }

    private fun triggerStateListener(state: PlaybackState): Unit =
      synchronized(controlListenerLock) {
        verifyMainThread()
        _playerState.value = state
        when (state) {
          is PlaybackState.BUFFERING -> {
            controlBufferListener.forEach { it(mediaController) }
            controlBufferListener.clear()
          }
          is PlaybackState.ENDED -> {
            controlEndedListener.forEach { it(mediaController) }
            controlEndedListener.clear()
          }
          is PlaybackState.IDLE -> {
            controlIdleListener.forEach { it(mediaController) }
            controlIdleListener.clear()
          }
          is PlaybackState.READY -> {
            controlReadyListener.forEach { it(mediaController) }
            controlReadyListener.clear()
          }
          is PlaybackState.UNIT -> {
            // we should never be here
            Timber.e("triggerListener with $state")
          }
        }
      }

    val newBitmapRequestException = CancellationException("new updateBitmapRequest")

    private fun setupController(controller: MediaController) {
      verifyMainThread()

      with(controller) {

        addListener(object : Player.Listener {

          override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val changeItem = {
              val item = mediaItem ?: MediaItem.EMPTY
              _playerMediaItem.value = item
              updateBitmapJob.cancel(newBitmapRequestException)
              updateBitmapJob = updateBitmap(item)
            }
            when (reason) {
              Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                if (mediaItem?.localConfiguration != null) return
                changeItem()
              }
              else -> changeItem()
            }
          }

          override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
              Player.STATE_IDLE -> {
                triggerStateListener(PlaybackState.IDLE)
              }
              Player.STATE_BUFFERING -> {
                triggerStateListener(PlaybackState.BUFFERING)
              }
              Player.STATE_READY -> {
                triggerStateListener(PlaybackState.READY)
              }
              Player.STATE_ENDED -> {
                triggerStateListener(PlaybackState.ENDED)
              }
            }
          }
        })
      }
    }

    private var updateBitmapJob = Job().job
    private var useArtUri = true // TODO
    private fun updateBitmap(item: MediaItem) = mainScope.launch {
      val bitmap = withContext(Dispatchers.IO) {
        itemHandler.getEmbeds(item)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
          ?: elseNull(useArtUri) {
            item.mediaMetadata.artworkUri?.let { coilHandler.makeBitmap(MediaItemUtil.showArtUri(it)) }
          }
      }?.let { BitmapDrawable(context.resources, it) }
      ensureActive()
      _playerBitmap.value = bitmap
    }



    private fun getPlayerVolume(): Float {
      verifyMainThread()
      return mediaController.volume
    }

    private fun setPlayerVolume(
      @FloatRange(from = 0.0, to = 1.0) f: Float
    ) {
      verifyMainThread()
      val fv = fixVolumeToRange(f)
      mediaController.volume = fv
      return Timber.d("setPlayerVolume to $f or $fv")
    }

    private val fadeLock = Any()

    @GuardedBy("fadeLock")
    @Volatile
    private var fading = false

    @GuardedBy("fadeLock")
    private val fadeListener = mutableListOf<ControllerCommand.WithFade>()

    val maxVolume = MAX_VOLUME

    suspend fun fadeOut(
      command: ControllerCommand.WithFade
    ) {
      verifyMainThread()
      synchronized(fadeLock) {
        if (command.flush) fadeListener.clear()
        fadeListener.add(command)

        if (fading || !mediaController.isPlaying) {
          fading = false
          fadeListener.forEachClear(fadeLock) { commandController(it.command) }
          return whenReady { setPlayerVolume(maxVolume) }
        }

        fading = true
      }

      val to = command.to
      val duration = command.duration.toFloat()
      val interval = command.interval.toFloat()
      val step = duration / interval
      val deltaVol = getPlayerVolume() / step

      while (getPlayerVolume() > to
        && fading
      ) {
        setPlayerVolume(getPlayerVolume() - deltaVol)
        delay(command.interval)
      }

      synchronized(fadeLock) {
        if (fading) {
          fading = false
          fadeListener.forEachClear(fadeLock) { commandController(it.command) }
          return whenReady { setPlayerVolume(maxVolume) }
        }
      }
    }
  }
}
