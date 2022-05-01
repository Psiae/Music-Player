package com.kylentt.mediaplayer.domain.mediasession

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.MediaServiceController
import com.kylentt.mediaplayer.domain.mediasession.service.connector.MediaServiceConnector
import com.kylentt.mediaplayer.domain.mediasession.service.connector.PlaybackState
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import timber.log.Timber
import java.lang.Exception
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Singleton that Handles [androidx.media3.session.MediaSession] and Expose necessary Information,
 * e.g: [PlaybackState] of [androidx.media3.common.Player] Interface
 * @constructor [appScope] [AppScope] for Async Task
 * @constructor [baseContext] [Context] to access App Resources
 * @constructor [coilHelper] [CoilHelper] for ImageLoading Task
 * @constructor [dispatchers] [AppDispatchers] for Async Task
 * @constructor [itemHelper] [MediaItemHelper] for MediaItem Task
 * @constructor [mediaRepo] [MediaRepository] for [com.kylentt.mediaplayer.data.SongEntity] Task
 * @constructor [protoRepo] [ProtoRepository] for [androidx.datastore.core.DataStore] Task
 * @see [MediaServiceConnector]
 * @see [MediaServiceController]
 * @see [MediaService]
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
class MediaSessionManager(
  private val appScope: AppScope,
  private val baseContext: Context,
  private val coilHelper: CoilHelper,
  private val dispatchers: AppDispatchers,
  private val itemHelper: MediaItemHelper,
  private val mediaRepo: MediaRepository,
  private val protoRepo: ProtoRepository
) {

  private val context = baseContext

  private val serviceConnector by lazy {
    MediaServiceConnector(
      appScope = appScope,
      baseContext = baseContext,
      coilHelper = coilHelper,
      dispatchers = dispatchers,
      itemHelper = itemHelper,
      protoRepo = protoRepo,
      sessionManager = this
    )
  }

  private val intentHandler by lazy {
    MediaIntentHandlerImpl(
      context = baseContext,
      dispatcher = dispatchers,
      itemHelper = itemHelper,
      mediaRepo = mediaRepo,
      protoRepo = protoRepo,
      sessionManager = this
    )
  }

  val itemBitmap = serviceConnector.itemBitmap
  val playbackState = serviceConnector.playbackState
  val serviceState = serviceConnector.serviceState

  @MainThread
  fun connectService() {
    serviceConnector.connectService()
  }

  @MainThread
  fun sendControllerCommand(command: ControllerCommand) {
    serviceConnector.commandController(command)
  }

  @OptIn(ExperimentalTime::class)
  suspend fun handleMediaIntent(intent: IntentWrapper) {
    try {
      if (!intent.shouldHandleIntent) return
      val time = measureTimedValue { intentHandler.handleMediaIntent(intent) }.duration
      Timber.d("handleMediaIntent handled in ${time.inWholeMilliseconds}ms")
    } catch (e: Exception) {
      Toast
        .makeText(context, "Unable To Play Media", Toast.LENGTH_LONG)
        .show()
    }
  }

  init {
    require(baseContext is Application)
  }
}
