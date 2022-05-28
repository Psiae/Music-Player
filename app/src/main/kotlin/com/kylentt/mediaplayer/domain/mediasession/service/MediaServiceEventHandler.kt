package com.kylentt.mediaplayer.domain.mediasession.service

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED
import androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.annotation.ThreadSafe
import com.kylentt.mediaplayer.core.delegates.Synchronize
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

interface MediaServiceEventHandler {
	@ThreadSafe
	fun handlePlayerMediaItemChanged(item: MediaItem, @Player.MediaItemTransitionReason reason: Int)
	@ThreadSafe
	fun handlePlayerStateChanged(state: @Player.State Int)
	@ThreadSafe
	fun handlePlayerRepeatModeChanged(@Player.RepeatMode mode: Int)
	@ThreadSafe
	fun handlePlayerError(player: Player, error: PlaybackException)
}

class MediaServiceEventHandlerImpl(
	private val service: MediaService
): MediaServiceEventHandler {

	val currentPlayer = service.currentSessionPlayer

	override fun handlePlayerMediaItemChanged(
		item: MediaItem,
		@Player.MediaItemTransitionReason reason: Int
	) {
		mediaItemChangedImpl(item, reason)
	}

	override fun handlePlayerStateChanged(state: Int) {
		playerStateChangedImpl(state)
	}

	override fun handlePlayerRepeatModeChanged(mode: Int) {
		playerRepeatModeChangedImpl(mode)
	}

	override fun handlePlayerError(player: Player, error: PlaybackException) {
		playerErrorImpl(player, error)
	}

	private var handlePlayerMediaItemChangedJob by Synchronize(Job().job)
	private fun mediaItemChangedImpl(item: MediaItem, reason: Int) {
		service.mainScope.launch {
			handlePlayerMediaItemChangedJob.cancel()
			handlePlayerMediaItemChangedJob = service.mainScope.launch {
				Timber.d("mediaItemChangedImpl ${item.getDebugDescription()}" +
					"\nreason: $reason")
				service.notificationProvider.suspendHandleMediaItemTransition(item, reason)
			}
		}
	}

	private var handlePlayerRepeatModeChanged by Synchronize(Job().job)
	private fun playerRepeatModeChangedImpl(mode: @Player.RepeatMode Int) {
		handlePlayerRepeatModeChanged.cancel()
		handlePlayerRepeatModeChanged = service.mainScope.launch {
			service.notificationProvider.suspendHandleRepeatModeChanged(mode)
		}
	}

	private var handlePlayerErrorJob by Synchronize(Job().job)
	private fun playerErrorImpl(player: Player, error: PlaybackException) {
		handlePlayerErrorJob.cancel()
		handlePlayerErrorJob = service.mainScope.launch {
			val code = error.errorCode

			@SuppressLint("SwitchIntDef")
			when (code) {
				ERROR_CODE_IO_FILE_NOT_FOUND -> handleIOErrorFileNotFound(player, error)
				ERROR_CODE_DECODING_FAILED -> {
					val context = service.applicationContext
					Toast.makeText(context, "Decoding Failed ($code), Paused", Toast.LENGTH_LONG).show()
				}
			}
		}
	}

	private var handlePlayerStateChangedJob by Synchronize(Job().job)
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
