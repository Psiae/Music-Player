package com.flammky.musicplayer.domain.musiclib.player

import android.content.Context
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flammky.android.medialib.temp.util.addResultListener
import com.flammky.musicplayer.domain.musiclib.service.MusicLibraryService
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

internal class MediaControllerWrapper(private val context: Context) {
	private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
	private lateinit var mediaController: MediaController

	private var mState: State = State.NOTHING

	private var localListeners: MutableList<Player.Listener> = mutableListOf()

	private val directExecutor = Executor { it.run() }

	val playing: Boolean
		get() = maybeControllerValue(false) { controller -> controller.isPlaying }

	val playWhenReady: Boolean
		get() = maybeControllerValue(false) { controller -> controller.playWhenReady}

	val playerState: @Player.State Int
		get() = maybeControllerValue(Player.STATE_IDLE) { controller -> controller.playbackState }

	val repeatMode: @Player.RepeatMode Int
		get() = maybeControllerValue(Player.REPEAT_MODE_OFF) { controller -> controller.repeatMode }

	val duration: Long
		get() = maybeControllerValue(-1L) { controller -> controller.duration }

	val position: Long
		get() = maybeControllerValue(-1L) { controller -> controller.currentPosition }

	val bufferedPosition: Long
		get() = maybeControllerValue(-1) { controller -> controller.bufferedPosition }

	val currentMediaItem: MediaItem?
		get() = maybeControllerValue(null) { controller -> controller.currentMediaItem }

	val hasNextMediaItem
		get() = maybeControllerValue(false) { controller -> controller.hasNextMediaItem() }

	val hasPreviousMediaItem
		get() = maybeControllerValue(false) { controller -> controller.hasPreviousMediaItem() }

	val nextMediaItem: MediaItem?
		get() = maybeControllerValue(null) { controller ->
			val nextIndex = controller.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET }
				?: return null
			controller.getMediaItemAt(nextIndex)
		}

	val previousMediaItem: MediaItem?
		get() = maybeControllerValue(null) { controller ->
			val previousIndex = controller.previousMediaItemIndex.takeIf { it != C.INDEX_UNSET }
				?: return null
			controller.getMediaItemAt(previousIndex)
		}

	fun prepareResource() {
		if (isConnected()) mediaController.prepare()
	}

	fun play() {
		if (isConnected()) mediaController.play()
	}

	fun pause() {
		if (isConnected()) mediaController.pause()
	}

	fun stop() {
		if (isConnected()) mediaController.stop()
	}

	fun seekToNextMediaItem() {
		if (isConnected()) mediaController.seekToNextMediaItem()
	}

	fun seekToPosition(position: Long) {
		if (isConnected()) mediaController.seekTo(position)
	}

	fun setMediaItem(item: MediaItem, startPosition: Long) {
		if (isConnected()) mediaController.setMediaItem(item, startPosition)
	}

	fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Long) {
		if (isConnected()) mediaController.setMediaItems(items, startIndex, startPosition)
	}

	fun getMediaItems(): List<MediaItem> = maybeControllerValue(emptyList()) { controller ->
		val itemCount = controller.mediaItemCount.takeIf { it > 0 }
			?: return emptyList()

		val items = mutableListOf<MediaItem>()

		/* I wonder which one should I use, but stick to the latter for now
		(0 until itemCount).forEach { index ->
			holder.add(controller.getMediaItemAt(index))
		}
		*/

		for (index in 0 until itemCount) {
			items.add(controller.getMediaItemAt(index))
		}

		items
	}

	fun isConnected(): Boolean = mState == State.CONNECTED
	fun isConnecting(): Boolean = mState == State.CONNECTING
	fun isDisconnected(): Boolean = mState == State.DISCONNECTED

	fun connect(onConnected: (MediaControllerWrapper) -> Unit = {}): Unit = connectServiceInternal {
		onConnected(this)
	}

	fun addListener(listener: Player.Listener) {
		localListeners.add(listener)
		if (isConnected()) mediaController.addListener(listener)
	}

	fun removeListener(listener: Listener) {
		localListeners.removeIf { it === listener }
	}

	private fun connectServiceInternal(onConnected: (MediaController) -> Unit = {}) {
		if (isConnected()) {
			return onConnected(mediaController)
		}
		if (isConnecting()) {
			return mediaControllerFuture.addResultListener(directExecutor, onConnected)
		}

		mState = State.CONNECTING

		val token = SessionToken(context, MusicLibraryService.getComponentName(context))

		mediaControllerFuture = MediaController.Builder(context, token)
			.setConnectionHints( /* Later */ Bundle.EMPTY)
			.buildAsync()

		mediaControllerFuture.addResultListener(directExecutor) { result: MediaController ->
			mediaController = result
			mState = State.CONNECTED
			localListeners.forEach { listener -> result.addListener(listener) }
			onConnected(result)
		}
	}

	private inline fun <R> maybeControllerValue(defaultValue: R, block: (MediaController) -> R): R {
		return if (isConnected()) block(mediaController) else defaultValue
	}

	// Convert to sealed class with ERROR data class if needed
	enum class State {
		NOTHING, CONNECTING, CONNECTED, DISCONNECTED
	}
}
