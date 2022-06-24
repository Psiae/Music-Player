package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.session.MediaController
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.coroutines.AppScope
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemHelper
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.helper.image.CoilHelper
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Connector between [MediaSessionConnector] and [MediaServiceController]
 * @author Kylentt
 * @since 2022/04/30
 * @see [MediaService]
 */

class ServiceConnector(
    @JvmField val appScope: AppScope,
    @JvmField val baseContext: Context,
    @JvmField val coilHelper: CoilHelper,
    @JvmField val dispatchers: AppDispatchers,
    @JvmField val itemHelper: MediaItemHelper,
    @JvmField val protoRepo: ProtoRepository,
    @JvmField val sessionManager: MediaSessionConnector
) {

  private val serviceController = MediaServiceController(this)

  val playbackState = serviceController.playbackStateSF.asStateFlow()
  val serviceState = serviceController.serviceStateSF.asStateFlow()

  @MainThread
  fun connectService(onConnected: (MediaController) -> Unit) {
    serviceController.connectController {
			Timber.d("Controller Connected")
			onConnected(it)
		}
  }

	@MainThread
	fun disconnectService() = serviceController.disconnectController()

  @MainThread
  fun commandController(command: ControllerCommand) {
    serviceController.commandController(command)
  }
}


