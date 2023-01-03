package com.flammky.android.medialib.player.lib.exo

import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.player.lib.LibPlayerContext
import com.flammky.android.medialib.player.lib.exo.ExoPlayerContext.Builder

/**
 * Context that defines the environment requirement for ExoPlayer.
 *
 * @see Builder to create instance
 */
class ExoPlayerContext private constructor(

	/**
	 * The Public Looper for this ExoPlayer.
	 *
	 * @see [Builder.looper]
	 */
	val looper: Looper?,

	/**
	 * The Library Context associated with this ExoPlayer
	 *
	 * @see [Builder.library]
	 */
	override val library: LibraryContext

) : LibPlayerContext() {

	override val android: AndroidContext = library.android

	internal fun buildExoPlayer(
		androidContext: AndroidContext,
		looper: Looper
	): ExoPlayer {
		return ExoPlayer.Builder(androidContext)
			.setLooper(looper)
			.build()
	}

	class Builder internal constructor(val library: LibraryContext) {

		var looper: Looper? = null
			private set

		fun setLooper(looper: Looper?) = apply {
			this.looper = looper
		}

		fun build(): ExoPlayerContext = ExoPlayerContext(looper, library)
	}

	companion object {
		fun build(library: LibraryContext, apply: Builder.() -> Unit): ExoPlayerContext {
			return Builder(library).apply(apply).build()
		}
	}
}
