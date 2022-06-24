package com.kylentt.mediaplayer.domain.mediasession

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.session.MediaController
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.coroutines.AppScope
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemHelper
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ServiceConnector
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.kylentt.mediaplayer.helper.image.CoilHelper
import timber.log.Timber
import java.lang.Exception
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Singleton
class MediaSessionConnector(
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
    ServiceConnector(
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

  val playbackState
    get() = serviceConnector.playbackState

  val serviceState
    get() = serviceConnector.serviceState

  @MainThread
  fun connectService(onConnected: (MediaController) -> Unit = {}) {
    serviceConnector.connectService(onConnected)
  }

	@MainThread
	fun disconnectService() {
		serviceConnector.disconnectService()
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
      Toast.makeText(context, "Unable To Play Media", Toast.LENGTH_LONG).show()
    }
  }

  init {
    checkArgument(baseContext is Application) {
      "Singleton Context must be Application"
    }
  }
}
