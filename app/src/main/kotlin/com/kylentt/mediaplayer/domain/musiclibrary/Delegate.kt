package com.kylentt.mediaplayer.domain.musiclibrary

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.core.coroutines.CoroutineDispatchers
import com.kylentt.mediaplayer.domain.musiclibrary.service.ServiceController
import javax.inject.Singleton

@Singleton
class MusicLibraryDelegate private constructor(
	private val context: Context,
	private val coroutineDispatchers: CoroutineDispatchers,
) {

	private val serviceConnector = ServiceController(context, coroutineDispatchers)

	val playbackStateSF
		get() = serviceConnector.playbackState

	@MainThread
	fun sendControllerCommand(command: ServiceController.ControllerCommand) {
		serviceConnector.sendControllerCommand(command)
	}

	companion object {
		@SuppressLint("StaticFieldLeak")
		private lateinit var instance: MusicLibraryDelegate

		fun get(context: Context, coroutineDispatchers: CoroutineDispatchers): MusicLibraryDelegate {
			if (!::instance.isInitialized) synchronized(this) {
				instance = MusicLibraryDelegate(context.applicationContext, coroutineDispatchers)
			}
			return instance
		}
	}
}
