package com.flammky.android.medialib.temp.api.player

import android.net.Uri
import android.os.Looper
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Extra.Companion.toMediaItemExtra
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.common.mediaitem.PlaybackMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isDelete
import com.flammky.android.medialib.temp.media3.internal.mediacontroller.MediaControllerWrapper
import com.flammky.android.medialib.temp.player.LibraryPlayer
import com.flammky.android.medialib.temp.player.PlayerContext
import com.google.common.util.concurrent.AbstractFuture
import com.google.common.util.concurrent.ListenableFuture
import kotlin.time.Duration.Companion.milliseconds

internal class ForwardingMediaController private constructor(
    private val playerContext: PlayerContext,
    private val wrapper: MediaControllerWrapper
) : MediaController, LibraryPlayer by wrapper {

	override val currentActualMediaItem: MediaItem?
		get() = wrapper.currentMediaItem?.let { convertMediaItem(it) }

	override fun post(block: MediaController.() -> Unit) {
		wrapper.post { block() }
	}

	override fun <R> postListen(block: MediaController.() -> R, listener: (R) -> Unit) {
		wrapper.postListen( { block() } ) { result -> listener(result) }
	}

	override fun <R> joinBlocking(block: MediaController.() -> R): R {
		return wrapper.joinBlocking { block() }
	}

	override fun <R> joinBlockingSuspend(block: suspend MediaController.() -> R): R {
		return wrapper.joinBlockingSuspend { block() }
	}

	override suspend fun <R> joinSuspend(block: suspend MediaController.() -> R): R {
		return wrapper.joinSuspend { block() }
	}

	override val publicLooper: Looper
		get() = wrapper.publicLooper

	@Deprecated("Temporary", ReplaceWith("TODO"))
	private fun convertMediaItem(item: androidx.media3.common.MediaItem): MediaItem {
		return playerContext.libContext.buildMediaItem {
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


	private var currentFuture: ListenableFuture<MediaController>? = null

	internal constructor(playerContext: PlayerContext) : this(playerContext, MediaControllerWrapper(playerContext))

	override val connected: Boolean
		get() = wrapper.isConnected()

	override fun connectService(): ListenableFuture<MediaController> {
		if (currentFuture == null) { currentFuture = WrapperFuture() }
		return currentFuture!!
	}

	inner class WrapperFuture : AbstractFuture<MediaController>() {
		init {
			wrapper.connect({}) {
				set(this@ForwardingMediaController)
			}
		}

		override fun set(value: MediaController?): Boolean {
			return super.set(value)
		}
	}
}
