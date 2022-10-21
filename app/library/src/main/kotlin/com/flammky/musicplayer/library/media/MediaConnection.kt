package com.flammky.musicplayer.library.media

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Media Connection Interface for this library module
 */
internal interface MediaConnection {
	fun play(id: String, uri: Uri)

	val repository: Repository

	interface Repository {
		suspend fun getArtwork(id: String): Any?
		fun observeArtwork(id: String): Flow<Any?>
		fun provideArtwork(id: String, artwork: Any?, silent: Boolean)
		fun evictArtwork(id: String, silent: Boolean)
	}
}
