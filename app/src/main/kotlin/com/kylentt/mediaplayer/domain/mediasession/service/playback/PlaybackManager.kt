package com.kylentt.mediaplayer.domain.mediasession.service.playback

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class MusicLibraryPlaybackManager(
	private val musicLibrary: MusicLibraryService,
	private val sessionManager: MusicLibrarySessionManager
) {

	private val playbackListener = PlayerPlaybackListener()
	private val eventHandler = PlayerPlaybackEventHandler()

	private val appDispatchers = AppModule.provideAppDispatchers()
	private val managerJob = SupervisorJob(musicLibrary.serviceJob)
	private val immediateScope = CoroutineScope(appDispatchers.mainImmediate + managerJob)

	private val serviceObserver = ServiceObserver()

	var isReleased: Boolean = false
		private set

	init {
		musicLibrary.lifecycle.addObserver(serviceObserver)
	}

	fun release(obj: Any) {
		Timber.d("$obj called release on PlaybackManager")

		if (isReleased) {
			return Timber.d("$obj, PlaybackManager is already released")
		}

		releaseImpl(obj)
	}

	private fun releaseImpl(obj: Any) {
		sessionManager.unRegisterPlayerEventListener(playbackListener)
		managerJob.cancel()

		isReleased = true

		Timber.i("$obj released PlaybackManager")
	}

	private fun onPlaybackError(error: PlaybackException) {
		if (isReleased) return

		sessionManager.getSessionPlayer()
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

			Timber.d("onPlaybackError, \nerror: ${error}\ncode: ${error.errorCode}\nplayer: $player")

			@SuppressLint("SwitchIntDef")
			when (error.errorCode) {
				PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(player, error)
				PlaybackException.ERROR_CODE_DECODING_FAILED -> {
					player.pause()
					val context = musicLibrary.applicationContext
					val toast = Toast.makeText(context, "Decoding Failed (${error.errorCode}), Paused", Toast.LENGTH_LONG)
					toast.show()
				}
			}
		}

		private suspend fun handleIOErrorFileNotFound(
			player: Player,
			error: PlaybackException
		) = withContext(appDispatchers.mainImmediate) {
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
						musicLibrary.applicationContext, uri
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

			val toast = Toast.makeText(musicLibrary.applicationContext,
				"Could Not Find Media ${ if (count > 1) "Files ($count)" else "File" }",
				Toast.LENGTH_LONG
			)

			toast.show()

			Timber.d("handleIOErrorFileNotFound handled")
		}

		// end of class
	}

	private inner class PlayerPlaybackListener : Player.Listener {
		override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)
	}

	private inner class ServiceObserver : LifecycleEventObserver {
		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			when (event) {
				ON_CREATE -> sessionManager.registerPlayerEventListener(playbackListener)
				ON_DESTROY -> release(this)
				else -> Unit
			}
		}
	}
}
