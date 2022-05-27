package com.kylentt.mediaplayer.domain.mediasession.service

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.delegates.LockMainThread
import com.kylentt.mediaplayer.core.exoplayer.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.core.exoplayer.MediaMetadataHelper.getStoragePath
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

interface MediaServiceListener {
	val playerLister: Player.Listener

	fun mediaItemTransition(item: MediaItem, @Player.MediaItemTransitionReason reason: Int)
	fun playerError(error: PlaybackException)
	fun repeatModeChanged(@Player.RepeatMode mode: Int)
}

class MediaServiceListenerImpl(private val service: MediaService): MediaServiceListener {

	val mediaRepository
		get() = service.mediaRepo

	val mediaSession
		get() = service.currentMediaSession

	val sessionPlayer
		get() = service.sessionPlayer

	val notificationProvider
		get() = service.notificationProvider

	private val playerListenerImpl = object : Player.Listener {

		override fun onPlaybackStateChanged(playbackState: Int) {
			super.onPlaybackStateChanged(playbackState)
			if (!playbackState.isOngoing()) {
				notificationProvider.considerForegroundService(mediaSession)
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)
			mediaItemTransition(mediaItem ?: MediaItem.EMPTY, reason)
		}

		override fun onPlayerError(error: PlaybackException) {
			super.onPlayerError(error)
			playerError(error)
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			repeatModeChanged(repeatMode)
		}
	}

	override val playerLister: Player.Listener
		get() = playerListenerImpl

	init {
		checkNotNull(service.baseContext) {
			"Make Sure to Initialize $this lazily after super.onCreate()"
		}
	}

	@SuppressLint("SwitchIntDef")
	override fun playerError(error: PlaybackException) {
		when (error.errorCode) {
			ERROR_CODE_IO_FILE_NOT_FOUND -> service.mainScope.launch {
				handlePlayerFileNotFound(service.sessionPlayer)
			}
		}
	}

	private var mediaItemTransitionJob by LockMainThread(Job().job)
	override fun mediaItemTransition(item: MediaItem, reason: Int) {
		mediaItemTransitionJob.cancel()
		mediaItemTransitionJob = service.mainScope.launch {
			service.notificationProvider.suspendHandleMediaItemTransition(item, reason)
		}
	}

	private var repeatModeChangedJob by LockMainThread(Job().job)
	override fun repeatModeChanged(mode: Int) {
		repeatModeChangedJob.cancel()
		repeatModeChangedJob = service.mainScope.launch {
			service.notificationProvider.suspendHandleRepeatModeChanged(mode)
		}
	}

	private fun handlePlayerFileNotFound(player: Player) {
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
						Timber.d("$e: Couldn't open Input Stream for $uri")
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
