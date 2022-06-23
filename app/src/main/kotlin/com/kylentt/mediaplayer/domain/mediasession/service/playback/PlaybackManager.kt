package com.kylentt.mediaplayer.domain.mediasession.service.playback

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

	init {
		sessionManager.registerPlayerEventListener(playbackListener)
	}


	private fun onPlaybackError(error: PlaybackException) {
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

			val items = getPlayerMediaItems(player)

			var count = 0
			items.forEachIndexed { index: Int, item: MediaItem ->

				val uri = item.mediaMetadata.mediaUri
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
					val fIndex = index - count
					Timber.d("$uriString doesn't Exist, removing ${item.getDebugDescription()}, " +
						"\nindex: was $index current $fIndex"
					)
					player.removeMediaItem(fIndex)
					count++
				}
			}
			val toast = Toast.makeText(musicLibrary.applicationContext,
				"Could Not Find Media ${ if (count > 1) "Files ($count)" else "File" }",
				Toast.LENGTH_LONG
			)
			toast.show()
		}
	}

	private inner class PlayerPlaybackListener : Player.Listener {
		override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)
	}
}
