package com.kylentt.mediaplayer.domain.mediasession.service.playback

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class PlaybackManager : MusicLibraryService.ServiceComponent.Stoppable() {
	private val playbackListener = PlayerPlaybackListener()
	private val eventHandler = PlayerPlaybackEventHandler()

	private lateinit var managerJob: Job
	private lateinit var dispatchers: AppDispatchers
	private lateinit var immediateScope: CoroutineScope

	private var isStarted = false
	private var isReleased = false

	override fun onCreate(serviceInteractor: MusicLibraryService.ServiceInteractor) {
		super.onCreate(serviceInteractor)
		managerJob = serviceInteractor.getCoroutineMainJob()
		dispatchers = serviceInteractor.getCoroutineDispatchers()
		immediateScope = CoroutineScope(dispatchers.mainImmediate + managerJob)
	}

	@MainThread
	override fun onStart(componentInteractor: MusicLibraryService.ComponentInteractor) {
		super.onStart(componentInteractor)
		componentInteractor.mediaSessionManagerDelegator.registerPlayerEventListener(playbackListener)
		isStarted = true
	}

	@MainThread
	override fun onStop(componentInteractor: MusicLibraryService.ComponentInteractor) {
		super.onStop(componentInteractor)
		componentInteractor.mediaSessionManagerDelegator.removePlayerEventListener(playbackListener)
		isStarted = false
	}

	@MainThread
	override fun onRelease(obj: Any) {
		super.onRelease(obj)

		Timber.i("PlaybackManager.release() called by $obj")

		managerJob.cancel()
		isReleased = true

		Timber.i("PlaybackManager released by $obj")
	}

	private fun onPlaybackError(error: PlaybackException) {
		if (isReleased) return

		componentInteractor?.mediaSessionManagerDelegator?.sessionPlayer
			?.let { immediateScope.launch { eventHandler.onPlaybackError(error, it) } }
	}

	private fun getPlayerMediaItems(player: Player): List<MediaItem> {
		val toReturn = mutableListOf<MediaItem>()
		for (i in 0 until player.mediaItemCount) {
			toReturn.add(player.getMediaItemAt(i))
		}
		return toReturn
	}

	private inner class PlayerPlaybackEventHandler {

		suspend fun onPlaybackError(error: PlaybackException, player: Player) {
			if (isReleased) return

			Timber.d("onPlaybackError, \nerror: ${error}\ncode: ${error.errorCode}\nplayer: $player")

			@SuppressLint("SwitchIntDef")
			when (error.errorCode) {
				PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(player, error)
				PlaybackException.ERROR_CODE_DECODING_FAILED -> {
					player.pause()
					val context = serviceInteractor?.getContext()!!.applicationContext
					val toast = Toast.makeText(
						context,
						"Decoding Failed (${error.errorCode}), Paused",
						Toast.LENGTH_LONG
					)
					toast.show()
				}
			}
		}

		private suspend fun handleIOErrorFileNotFound(
			player: Player,
			error: PlaybackException
		) = withContext(dispatchers.mainImmediate) {
			if (isReleased) return@withContext
			val currentContext = serviceInteractor?.getContext()!!

			Preconditions.checkArgument(error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)

			Timber.d("handleIOErrorFileNotFound")

			val items = getPlayerMediaItems(player)
			val indexToRemove = mutableListOf<Int>()

			var count = 0

			items.forEachIndexed { index: Int, item: MediaItem ->

				ensureActive()

				val uri = item.mediaUri
					?: Uri.parse(item.mediaMetadata.getStoragePath() ?: "")

				val uriString = uri.toString()

				val isExist = when {
					uriString.startsWith(DocumentProviderHelper.storagePath) -> File(uriString).exists()
					ContentProvidersHelper.isSchemeContentUri(uri) -> ContentProvidersHelper.isContentUriExist(
						currentContext.applicationContext, uri
					)
					else -> false
				}

				if (!isExist) {
					indexToRemove.add(index - count)
					Timber.d("$uriString doesn't Exist, removing ${item.getDebugDescription()}")
					count++
				}
			}

			ensureActive()

			indexToRemove.forEach { player.removeMediaItem(it) }

			val toast = Toast.makeText(
				currentContext.applicationContext,
				"Could Not Find Media ${if (count > 1) "Files ($count)" else "File"}",
				Toast.LENGTH_LONG
			)

			toast.show()

			Timber.d("handleIOErrorFileNotFound handled")
		}
	}

	inner class Delegate {
		// TODO
	}

	private inner class PlayerPlaybackListener : Player.Listener {
		override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)
	}

	private inner class ServiceObserver : LifecycleEventObserver {
		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			when (event) {
				ON_DESTROY -> onRelease(this)
				else -> Unit
			}
		}
	}
}
