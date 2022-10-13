package com.flammky.android.medialib.player.lib.exo

import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Extra.Companion.toMediaItemExtra
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.common.mediaitem.PlaybackMetadata
import com.flammky.android.medialib.errorprone.UnsafeBySuspend
import com.flammky.android.medialib.media3.Media3Item
import com.flammky.android.medialib.player.Player
import com.flammky.android.medialib.player.ThreadLockedPlayer
import com.flammky.android.medialib.player.lib.LibPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ExoPlayerWrapper(override val context: ExoPlayerContext)
	: LibPlayer(), ThreadLockedPlayer<ExoPlayerWrapper>  {

	private var released: Boolean = false

	/**
	 * Handler Thread for this player in case [context] looper is null so we create one ourselves
	 */
	private val handlerThread: HandlerThread? =
		if (context.looper == null) {
			ThreadFactory.createHandlerThread()
		} else {
			null
		}

	/**
	 * Actual non-null Looper
	 */
	private val looper = context.looper ?: handlerThread!!.looper

	/**
	 * Handler for our looper
	 */
	private val looperHandler = Handler(looper)

	/**
	 * Coroutine Dispatcher for our Handler
	 */
	private val dispatcher = looperHandler.asCoroutineDispatcher()

	/**
	 * Coroutine Scope for our Dispatcher
	 */
	private val scope = CoroutineScope(dispatcher + SupervisorJob())

	/**
	 * The Exoplayer instance this player wraps
	 */
	private val exoPlayer = context.buildExoPlayer(context.android, looper)


	override val bufferedPosition: Duration
		get() = joinBlocking {
			fixUnsetPosition(exoPlayer.bufferedPosition)
		}

	override val retainedBufferedPosition: Duration
		get() = joinBlocking {
			/* TODO */
			fixUnsetPosition(-1)
		}

	override val position: Duration
		get() = joinBlocking {
			fixUnsetPosition(exoPlayer.currentPosition)
		}

	override val duration: Duration
		get() = joinBlocking {
			fixUnsetDuration(exoPlayer.duration)
		}

	override var playWhenReady: Boolean
		get() = joinBlocking {
			exoPlayer.playWhenReady
		}
		set(value) = joinBlocking {
			exoPlayer.playWhenReady = value
		}

	override val isPlaying: Boolean
		get() = joinBlocking {
			exoPlayer.isPlaying
		}

	override val mediaItem: MediaItem?
		get() = joinBlocking {
			exoPlayer.currentMediaItem?.let { convertMediaItem(it) }
		}

	override val currentMediaItemIndex: Int
		get() = joinBlocking {
			fixUnsetIndex(exoPlayer.currentMediaItemIndex)
		}

	override val state: Player.State
		get() = joinBlocking {
			fixPlayerState(exoPlayer.playbackState)
		}

	override val isReleased: Boolean
		get() = joinBlocking {
			released
		}

	override fun play() {
		post { exoPlayer.play() }
	}

	override fun prepare() {
		post { exoPlayer.prepare() }
	}

	override fun pause() {
		post { exoPlayer.pause() }
	}

	override fun stop() {
		post { exoPlayer.stop() }
	}

	override fun release() {
		post { exoPlayer.release() }
	}

	override fun setMediaItem(item: MediaItem) {
		post { exoPlayer.setMediaItem((item as Media3Item).item) }
	}

	override fun post(block: ExoPlayerWrapper.() -> Unit) {
		scope.launch(dispatcher.immediate) { block() }
	}

	override fun <R> postListen(block: ExoPlayerWrapper.() -> R, listener: (R) -> Unit) {
		scope.launch(dispatcher.immediate) { listener(block()) }
	}

	override fun <R> joinBlocking(block: ExoPlayerWrapper.() -> R): R {
		return if (inLooper()) {
			block()
		} else {

			// The value, HOLD singleton object as placeholder as its a private object
			// there's no possibility of R === HOLD externally
			var value: Any? = HOLD

			// The current Thread
			val thread = Thread.currentThread()

			// Post the block to the looper queue and listen for result
			postListen(block) { result ->
				value = result

				// value is set, un-park the Thread
				LockSupport.unpark(thread)
			}

			// keep it parked until value is set.
			// we can also add additional park limit time, but return signature will be nullable R?
			while (value === HOLD) {
				LockSupport.park(this)
			}

			return value as R
		}
	}

	@UnsafeBySuspend
	override fun <R> joinBlockingSuspend(block: suspend ExoPlayerWrapper.() -> R): R {
		val context = if (inLooper()) EmptyCoroutineContext else dispatcher.immediate
		return runBlocking(context) { block() }
	}

	override suspend fun <R> joinSuspend(block: suspend ExoPlayerWrapper.() -> R): R {
		return if (inLooper()) {
			block()
		} else {
			withContext(dispatcher.immediate) { block() }
		}
	}

	override val publicLooper: Looper
		get() = looper

	private fun inLooper(): Boolean = Looper.myLooper() == looper

	private fun fixUnsetDuration(ms: Long): Duration {
		return if (ms < 0) Contract.DURATION_UNSET else ms.milliseconds
	}

	private fun fixUnsetPosition(ms: Long): Duration {
		return if (ms < 0) Contract.POSITION_UNSET else ms.milliseconds
	}

	private fun fixUnsetIndex(index: Int): Int {
		return if (index < 0) Contract.INDEX_UNSET else index
	}

	private fun fixPlayerState(@androidx.media3.common.Player.State state: Int): Player.State {
		return when (state) {
			androidx.media3.common.Player.STATE_IDLE -> Player.State.IDLE
			androidx.media3.common.Player.STATE_BUFFERING -> Player.State.BUFFERING
			androidx.media3.common.Player.STATE_ENDED -> Player.State.ENDED
			androidx.media3.common.Player.STATE_READY -> Player.State.READY
			else -> throw IllegalArgumentException("Invalid Player State conversion")
		}
	}

	@Deprecated("Temporary", ReplaceWith("TODO"))
	private fun convertMediaItem(item: androidx.media3.common.MediaItem): MediaItem {
		return context.library.buildMediaItem {
			setMediaUri(item.localConfiguration?.uri ?: item.requestMetadata.mediaUri ?: Uri.EMPTY)
			setMediaId(item.mediaId)
			setExtra(item.requestMetadata.extras?.toMediaItemExtra() ?: MediaItem.Extra.UNSET)

			val hint = item.requestMetadata.extras!!.getString("mediaMetadataType")!!
			val mediaMetadata = item.mediaMetadata

			val metadata = when {
				hint.startsWith("audio;") -> {
					AudioMetadata.build {
						mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
						setAlbumArtist(mediaMetadata.albumArtist?.toString())
						setAlbumTitle(mediaMetadata.albumTitle?.toString())
						setArtist(mediaMetadata.artist?.toString())
						setBitrate(mediaMetadata.extras?.getString("bitrate")?.toLong())
						setDuration(mediaMetadata.extras?.getString("durationMs")?.toLong()?.milliseconds)
						setPlayable(mediaMetadata.isPlayable)
						setTitle(mediaMetadata.title?.toString())
					}
				}
				hint.startsWith("playback;") -> {
					PlaybackMetadata.build {
						mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
						setBitrate(mediaMetadata.extras?.getString("bitrate")?.toLong())
						setDuration(mediaMetadata.extras?.getString("durationMs")?.toLong()?.milliseconds)
						setPlayable(mediaMetadata.isPlayable)
						setTitle(mediaMetadata.title?.toString())
					}
				}
				hint.startsWith("base;") -> {
					MediaMetadata.build {
						mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
						setTitle(mediaMetadata.title?.toString())
					}
				}
				else -> error("Invalid Media3Item Internal Info")
			}

			setMetadata(metadata)
		}
	}

	object ThreadFactory {
		const val name = "ExoPlayerWrapper"

		@get:Synchronized
		@set:Synchronized
		private var count = 0

		fun createHandlerThread(): HandlerThread = HandlerThread(name + ++count)
	}

	private object HOLD
}
