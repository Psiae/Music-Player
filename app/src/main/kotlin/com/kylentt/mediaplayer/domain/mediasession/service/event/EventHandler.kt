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
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper.Companion.orEmpty
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.IOException

class MusicLibraryEventHandler(
	private val service: MusicLibraryService,
	private val mediaNotificationProvider: MusicLibraryNotificationProvider
) {

	private val dispatchers: AppDispatchers
		get() = service.coroutineDispatchers

	suspend fun handlePlayerPlayWhenReadyChanged(
		session: MediaSession
	): Unit = playWhenReadyChangedImpl(session)

	suspend fun handlePlayerMediaItemChanged(
		session: MediaSession,
		@Player.MediaItemTransitionReason reason: Int
	): Unit = mediaItemChangedImpl(session, reason)

	suspend fun handlePlayerStateChanged(
		session: MediaSession
	): Unit = playerStateChangedImpl(session)

	suspend fun handlePlayerRepeatModeChanged(
		session: MediaSession
	): Unit = playerRepeatModeChangedImpl(session)

	suspend fun handlePlayerError(
		session: MediaSession,
		error: PlaybackException
	): Unit = playerErrorImpl(session, error)

	suspend fun handleServiceRelease(): Unit {
		mediaNotificationProvider.release()
	}

	private suspend fun playWhenReadyChangedImpl(
		session: MediaSession
	): Unit = withContext(dispatchers.main) {
		mediaNotificationProvider.launchSessionNotificationValidator(session) { notification ->
			ensureActive()
			mediaNotificationProvider.updateSessionNotification(notification)
		}
	}

	private suspend fun mediaItemChangedImpl(
		session: MediaSession,
		reason: @Player.MediaItemTransitionReason Int
	) = withContext(dispatchers.main) {
		val item = session.player.currentMediaItem.orEmpty()
		mediaNotificationProvider.suspendHandleMediaItemTransition(item, reason)
	}

	private suspend fun playerRepeatModeChangedImpl(
		session: MediaSession
	) = withContext(dispatchers.main) {
		mediaNotificationProvider.suspendHandleRepeatModeChanged(session.player.repeatMode)
	}

	private suspend fun playerErrorImpl(
		session: MediaSession,
		error: PlaybackException
	) = withContext(dispatchers.main) {
		val code = error.errorCode

		@SuppressLint("SwitchIntDef")
		when (code) {
			ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(session, error)
			ERROR_CODE_DECODING_FAILED -> {
				session.player.pause()
				val context = service.applicationContext
				Toast.makeText(context, "Decoding Failed ($code), Paused", Toast.LENGTH_LONG).show()
			}
		}
	}

	private suspend fun playerStateChangedImpl(
		session: MediaSession
	) = withContext(dispatchers.main) {
		val notification = mediaNotificationProvider.getSessionNotification(session)
		mediaNotificationProvider.considerForegroundService(session.player, notification, false)
		mediaNotificationProvider.launchSessionNotificationValidator(session, 500) {}
	}

	@MainThread
	private suspend fun handleIOErrorFileNotFound(
		session: MediaSession,
		error: PlaybackException
	) = withContext(dispatchers.main) {
		checkArgument(error.errorCode == ERROR_CODE_IO_FILE_NOT_FOUND)

		val items = getPlayerMediaItems(session.player)

		var count = 0
		items.forEachIndexed { index: Int, item: MediaItem ->
			val uri = item.mediaMetadata.mediaUri
				?: Uri.parse(item.mediaMetadata.getStoragePath() ?: "")
			val uriString = uri.toString()
			val isExist = when {
				uriString.startsWith(DocumentProviderHelper.storagePath) -> File(uriString).exists()
				ContentProvidersHelper.isSchemeContentUri(uri) -> {
					ContentProvidersHelper.isContentUriExist(service.applicationContext, uri)
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

	private fun getPlayerMediaItems(player: Player): List<MediaItem> {
		val toReturn = mutableListOf<MediaItem>()
		for (i in 0 until player.mediaItemCount) {
			toReturn.add(player.getMediaItemAt(i))
		}
		return toReturn
	}
}
