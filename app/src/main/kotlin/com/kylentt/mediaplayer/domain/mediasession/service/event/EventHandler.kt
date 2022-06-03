package com.kylentt.mediaplayer.domain.mediasession.service.event

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED
import androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper.Companion.orEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

class MusicLibraryEventHandler(
	private val service: MusicLibraryService
) {

	private val dispatcher = service.coroutineDispatchers

	suspend fun handlePlayerPlayWhenReadyChanged(
		session: MediaSession
	) = withContext(dispatcher.main) {
		ensureActive()
		playWhenReadyChangedImpl(session).join()
	}

	suspend fun handlePlayerMediaItemChanged(
		session: MediaSession,
		@Player.MediaItemTransitionReason reason: Int
	) = withContext(dispatcher.main) {
		ensureActive()
		mediaItemChangedImpl(session, reason).join()
	}

	suspend fun handlePlayerStateChanged(
		session: MediaSession
	) = withContext(dispatcher.main) {
		ensureActive()
		playerStateChangedImpl(session).join()
	}

	suspend fun handlePlayerRepeatModeChanged(
		session: MediaSession
	) = withContext(dispatcher.main) {
		ensureActive()
		playerRepeatModeChangedImpl(session).join()
	}

	suspend fun handlePlayerError(
		session: MediaSession,
		error: PlaybackException
	) = withContext(dispatcher.main) {
		ensureActive()
		playerErrorImpl(session, error).join()
	}

	private suspend fun playWhenReadyChangedImpl(
		session: MediaSession
	): Job = withContext(dispatcher.main) {
		launch {
			service.mediaNotificationProvider.launchSessionNotificationValidator(session) {
				service.mediaNotificationProvider.updateSessionNotification(it)
			}
		}
	}

	private suspend fun mediaItemChangedImpl(
		session: MediaSession,
		reason: @Player.MediaItemTransitionReason Int
	): Job = withContext(dispatcher.main) {
		launch {
			val item = session.player.currentMediaItem.orEmpty()
			service.mediaNotificationProvider.suspendHandleMediaItemTransition(item, reason)
		}
	}

	private suspend fun playerRepeatModeChangedImpl(
		session: MediaSession
	): Job = withContext(dispatcher.main) {
		launch {
			service.mediaNotificationProvider.suspendHandleRepeatModeChanged(session.player.repeatMode)
		}
	}

	private suspend fun playerErrorImpl(
		session: MediaSession,
		error: PlaybackException
	): Job = withContext(dispatcher.main) {
		launch {
			val code = error.errorCode

			@SuppressLint("SwitchIntDef")
			when (code) {
				ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(session, error).join()
				ERROR_CODE_DECODING_FAILED -> {
					session.player.pause()
					val context = service.applicationContext
					Toast.makeText(context, "Decoding Failed ($code), Paused", Toast.LENGTH_LONG).show()
				}
			}
		}
	}

	private suspend fun playerStateChangedImpl(
		session: MediaSession
	): Job = withContext(dispatcher.main) {
		launch {
			if (!session.player.playbackState.isOngoing()) {
				service.mediaNotificationProvider.launchSessionNotificationValidator(session) {}
			}
		}
	}

	@MainThread
	private suspend fun handleIOErrorFileNotFound(
		session: MediaSession,
		error: PlaybackException
	): Job = withContext(dispatcher.main) {
		launch {
			checkArgument(error.errorCode == ERROR_CODE_IO_FILE_NOT_FOUND)

			val items = getPlayerMediaItems(session.player)

			var count = 0
			items.forEachIndexed { index: Int, item: MediaItem ->
				val uri = item.mediaMetadata.mediaUri
					?: Uri.parse(item.mediaMetadata.getStoragePath() ?: "")
				val uriString = uri.toString()
				val isExist = when {
					uriString.startsWith(DocumentProviderHelper.storagePath) -> File(uriString).exists()
					uriString.startsWith(ContentProvidersHelper.contentScheme) -> {
						try {
							val iStream = service.applicationContext.contentResolver.openInputStream(uri)
							iStream!!.close()
							true
						} catch (e: IOException) {
							Timber.e("$e: Couldn't use Input Stream on $uri")
							false
						}
					}
					else -> false
				}

				if (!isExist) {
					val fIndex = index - count
					Timber.d("$uriString doesn't Exist, removing ${item.getDebugDescription()}, " +
						"\nindex: was $index current $fIndex"
					)
					session.player.removeMediaItem(fIndex)
					count++
				}
			}
			val toast = Toast.makeText(service.applicationContext,
				"Could Not Find Media ${ if (count > 1) "Files ($count)" else "File" }",
				Toast.LENGTH_LONG
			)
			toast.show()
		}
	}

	private fun getPlayerMediaItems(player: Player): List<MediaItem> {
		val toReturn = mutableListOf<MediaItem>()
		for (i in 0 until player.mediaItemCount) {
			toReturn.add(player.getMediaItemAt(i))
		}
		return toReturn
	}
}
