package com.kylentt.musicplayer.domain.mediasession

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.session.MediaLibraryService
import com.kylentt.musicplayer.app.util.AppScope
import com.kylentt.musicplayer.domain.helper.Preconditions.verifyMainThread
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Singleton


// Current Media Playback Single Info Source
internal class MediaSessionManager private constructor(
  private val base: Context,
  private val appScope: AppScope
) {
  private val mainScope = appScope.mainScope

  private var mediaServiceConnector: MediaServiceConnector
  private val controller
    get() = mediaServiceConnector.mediaServiceController
  val bitmapState
    get() = mediaServiceConnector.playerBitmap
  val itemState
    get() = mediaServiceConnector.playerMediaItem
  val playbackState
    get() = mediaServiceConnector.playerState
  val serviceState
    get() = mediaServiceConnector.serviceState

  init {
    require(base is Application) { "MediaSessionManager Invalid Context" }
    mediaServiceConnector = MediaServiceConnector(this, base, appScope)
  }

  @MainThread
  fun connectService() {
    verifyMainThread()
    controller.commandController(ControllerCommand.Unit)
  }

  @MainThread
  fun sendCommand(command: ControllerCommand) {
    verifyMainThread()
    controller.commandController(command)
  }

  // In case I'm lazy enough to not switch nor manage Context :)
  suspend fun mainSendCommand(command: ControllerCommand) =
    withContext(Dispatchers.Main) { sendCommand(command) }

  suspend fun mainConnectService() = withContext(Dispatchers.Main) { connectService() }
  suspend fun outScopeSendCommand(command: ControllerCommand) =
    mainScope.launch { mainSendCommand(command) }

  suspend fun outScopeConnectService() = mainScope.launch { mainConnectService() }

  companion object {
    fun build(context: Context, scope: AppScope) = MediaSessionManager(context, scope)
  }
}
