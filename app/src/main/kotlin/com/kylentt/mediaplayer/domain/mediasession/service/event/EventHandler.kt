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
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper.Companion.orEmpty
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import kotlin.coroutines.coroutineContext

class MusicLibraryEventHandler(
	private val service: MusicLibraryService
) {

	private val dispatcher = service.coroutineDispatchers

	suspend fun handlePlayerMediaItemChanged(
		session: MediaSession,
		@Player.MediaItemTransitionReason reason: Int
	) = withContext(dispatcher.main) {
		ensureActive()
		mediaItemChangedImpl(session.player.currentMediaItem.orEmpty(), reason)
	}

	suspend fun handlePlayerStateChanged(
		session: MediaSession
	) = withContext(dispatcher.main) {
		ensureActive()
		playerStateChangedImpl(session.player.playbackState)
	}

	suspend fun handlePlayerRepeatModeChanged(
		session: MediaSession
	) = withContext(dispatcher.main) {
		ensureActive()
		playerRepeatModeChangedImpl(session.player.playbackState)
	}

	suspend fun handlePlayerError(
		session: MediaSession,
		error: PlaybackException
	) = withContext(dispatcher.main) {
		ensureActive()
		playerErrorImpl(session.player, error)
	}

	private var handlePlayerMediaItemChangedJob = Job().job
	private fun mediaItemChangedImpl(item: MediaItem, reason: Int) {
		handlePlayerMediaItemChangedJob.cancel()
		handlePlayerMediaItemChangedJob = service.mainScope.launch {
			service.mediaNotificationProvider.suspendHandleMediaItemTransition(item, reason)
			Timber.d("mediaItemChangedImpl ${item.getDebugDescription()}" +
				"\nreason: $reason")
		}
	}

	private var handlePlayerRepeatModeChanged = Job().job
	private fun playerRepeatModeChangedImpl(mode: @Player.RepeatMode Int) {
		handlePlayerRepeatModeChanged.cancel()
		handlePlayerRepeatModeChanged = service.mainScope.launch {
			service.mediaNotificationProvider.suspendHandleRepeatModeChanged(mode)
		}
	}

	private var handlePlayerErrorJob = Job().job
	private fun playerErrorImpl(player: Player, error: PlaybackException) {
		handlePlayerErrorJob.cancel()
		handlePlayerErrorJob = service.mainScope.launch {
			val code = error.errorCode

			@SuppressLint("SwitchIntDef")
			when (code) {
				ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(player, error)
				ERROR_CODE_DECODING_FAILED -> {
					player.pause()
					val context = service.applicationContext
					Toast.makeText(context, "Decoding Failed ($code), Paused", Toast.LENGTH_LONG).show()
				}
			}
		}
	}

	private var handlePlayerStateChangedJob = Job().job
	private fun playerStateChangedImpl(state: @Player.State Int) {
		// TODO
	}

	@MainThread
	private fun handleIOErrorFileNotFound(player: Player, error: PlaybackException) {
		checkArgument(error.errorCode == ERROR_CODE_IO_FILE_NOT_FOUND)

		val items = getPlayerMediaItems(player)

		var count = 0
		items.forEachIndexed { index: Int, item: MediaItem ->
			val uri = item.mediaMetadata.mediaUri ?: Uri.parse(item.mediaMetadata.getStoragePath() ?: "")
			val uriString = uri.toString()
			val isExist = when {
				uriString.startsWith(DocumentProviderHelper.storagePath) -> File(uriString).exists()
				uriString.startsWith(ContentProvidersHelper.contentScheme) -> {
					try {
						val iStream = service.applicationContext.contentResolver.openInputStream(uri)
						iStream!!.close()
						true
					} catch (e: Exception) {
						Timber.d("$e: Couldn't use Input Stream on $uri")
						false
					}
				}
				else -> false
			}
			if (!isExist) {
				val fixedIndex = index - count
				Timber.d("$uriString doesn't Exist, removing ${item.getDebugDescription()}, " +
					"\nindex: was $index current $fixedIndex"
				)
				player.removeMediaItem(fixedIndex)
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
