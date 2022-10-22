package com.flammky.musicplayer.library.media

import android.net.Uri
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Media Connection Interface for this library module
 */
internal interface MediaConnection {
	fun play(id: String, uri: Uri)

	val repository: Repository

	interface Repository {
		suspend fun getArtwork(id: String): Any?
		suspend fun observeArtwork(id: String): Flow<Any?>
		suspend fun provideArtwork(id: String, artwork: Any?, silent: Boolean)
		suspend fun evictArtwork(id: String, silent: Boolean)

		suspend fun provideMetadata(id: String, metadata: MediaMetadata)
		suspend fun observeMetadata(id: String): Flow<MediaMetadata?>
	}
}
