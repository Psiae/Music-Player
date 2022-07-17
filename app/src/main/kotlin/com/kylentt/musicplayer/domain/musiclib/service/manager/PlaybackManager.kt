package com.kylentt.musicplayer.domain.musiclib.service.manager

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.app.dependency.AppModule
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaMetadataHelper.getStoragePath
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class PlaybackManager : MusicLibraryService.ServiceComponent() {
	private val playbackListener = PlayerPlaybackListener()
	private val eventHandler = PlayerPlaybackEventHandler()
	private val appDispatchers = AppModule.provideAppDispatchers()

	private lateinit var managerJob: Job
	private lateinit var mainScope: CoroutineScope

	private val sessionPlayer: Player?
		get() = if (!isStarted) null else componentDelegate.sessionInteractor.sessionPlayer

	override fun create(serviceDelegate: MusicLibraryService.ServiceDelegate) {
		super.create(serviceDelegate)
		val serviceProperty = serviceDelegate.propertyInteractor
		val serviceJob = serviceProperty.serviceMainJob
		managerJob = SupervisorJob(serviceJob)
		mainScope = CoroutineScope(appDispatchers.main + managerJob)
	}

	override fun release() {
		managerJob.cancel()
		if (isStarted) {
			componentDelegate.sessionInteractor.removePlayerEventListener(playbackListener)
		}
	}

	private fun onPlaybackError(error: PlaybackException) {
		sessionPlayer?.let { mainScope.launch { eventHandler.onPlaybackError(error, it) } }
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
			if (!isStarted) return
			val currentContext = serviceDelegate.propertyInteractor.context ?: return

			Timber.d("onPlaybackError, \nerror: ${error}\ncode: ${error.errorCode}\nplayer: $player")

			@SuppressLint("SwitchIntDef")
			when (error.errorCode) {
				PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(player, error)
				PlaybackException.ERROR_CODE_DECODING_FAILED -> {
					player.pause()
					val context = currentContext.applicationContext
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
		) = withContext(appDispatchers.mainImmediate) {
			if (!isStarted) return@withContext
			val currentContext = serviceDelegate.propertyInteractor.context ?: return@withContext

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

	private inner class PlayerPlaybackListener : Player.Listener {
		override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)
	}
}
