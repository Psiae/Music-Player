package com.flammky.android.medialib.temp.api.player

import android.os.Looper
import com.flammky.android.medialib.temp.media3.internal.mediacontroller.MediaControllerWrapper
import com.flammky.android.medialib.temp.player.LibraryPlayer
import com.flammky.android.medialib.temp.player.PlayerContext
import com.google.common.util.concurrent.AbstractFuture
import com.google.common.util.concurrent.ListenableFuture

internal class ForwardingMediaController private constructor(
    private val playerContext: PlayerContext,
    private val wrapper: MediaControllerWrapper
) : MediaController, LibraryPlayer by wrapper {


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

	override suspend fun <R> joinSuspending(block: suspend MediaController.() -> R): R {
		return wrapper.joinSuspending { block() }
	}

	override val publicLooper: Looper
		get() = TODO("Not yet implemented")
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
