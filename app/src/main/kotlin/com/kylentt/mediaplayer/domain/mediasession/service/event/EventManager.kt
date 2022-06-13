package com.kylentt.mediaplayer.domain.mediasession.service.event

import androidx.annotation.MainThread
import androidx.media3.common.Player
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.OnChanged
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class MusicLibraryEventManager(
	private val musicService: MusicLibraryService,
	notificationProvider: MusicLibraryNotificationProvider
) {

	private val eventHandler: MusicLibraryEventHandler
	private val eventListener: MusicLibraryServiceListener

	private val managerJob = SupervisorJob(musicService.serviceJob)

	var isReleased = false
		private set

	var isRunning = false
		private set

	val baseContext
		get() = musicService.baseContext

	val coroutineDispatchers: AppDispatchers = AppModule.provideAppDispatchers()

	val mainImmediateScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.mainImmediate + managerJob)

	val mainScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.main + managerJob)

	val mediaSession
		get() = musicService.currentMediaSession

	val isDependencyInjected
		get() = musicService.isDependencyInjected

	val lifecycle
		get() = musicService.lifecycle

	val serviceEventSF
		get() = musicService.serviceEventSF

	init {
		eventHandler = MusicLibraryEventHandler(this, notificationProvider)
		eventListener = MusicLibraryServiceListener(this, eventHandler)
	}

	private fun startImpl(stopSelf: Boolean, releaseSelf: Boolean) {
		eventListener.start(stopSelf, releaseSelf)
	}

	private fun releaseImpl(obj: Any) {
		managerJob.cancel()
		eventListener.release(obj)

		isReleased = true
	}

	fun start(stopSelf: Boolean, releaseSelf: Boolean) {
		if (!isRunning && !isReleased) {
			startImpl(stopSelf, releaseSelf)
		}
	}

	fun release(obj: Any) {
		if (!this.isReleased) {
			releaseImpl(obj)
		}
	}

	fun registerPlayerChangedListener(onChanged: OnChanged<Player>) {
		musicService.registerOnPlayerChanged(onChanged)
	}

	fun unregisterPlayerChangedListener(onChanged: OnChanged<Player>): Boolean {
		return musicService.unRegisterOnPlayerChanged(onChanged)
	}
}
