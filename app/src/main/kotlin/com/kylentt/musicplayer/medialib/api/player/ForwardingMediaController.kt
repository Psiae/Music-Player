package com.kylentt.musicplayer.medialib.api.player

import com.google.common.util.concurrent.AbstractFuture
import com.google.common.util.concurrent.ListenableFuture
import com.kylentt.musicplayer.medialib.media3.internal.mediacontroller.MediaControllerWrapper
import com.kylentt.musicplayer.medialib.player.LibraryPlayer
import com.kylentt.musicplayer.medialib.player.PlayerContext

internal class ForwardingMediaController private constructor(
	private val playerContext: PlayerContext,
	private val wrapper: MediaControllerWrapper
) : MediaController, LibraryPlayer by wrapper {

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
