package com.flammky.musicplayer.base.media.r

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface MediaConnectionRepository {
	fun getMetadataBlocking(id: String): MediaMetadata?
	suspend fun getMetadata(id: String): MediaMetadata?
	suspend fun observeMetadata(id: String): Flow<MediaMetadata?>
	suspend fun provideMetadata(id: String, metadata: MediaMetadata)
	suspend fun evictMetadata(id: String)

	/**
	 * Provide Metadata silently, meaning that this will not notify existing observers
	 */
	suspend fun silentProvideMetadata(id: String, metadata: MediaMetadata)

	/**
	 * Evict Metadata silently, meaning that this will not notify existing observers
	 */
	suspend fun silentEvictMetadata(id: String)

	suspend fun getArtwork(id: String): Any?
	suspend fun observeArtwork(id: String): Flow<Any?>
	suspend fun provideArtwork(id: String, artwork: Any?)
	suspend fun evictArtwork(id: String)

	/**
	 * Provide Artwork silently, meaning that this will not notify existing observers
	 */
	suspend fun silentProvideArtwork(id: String, artwork: Any?)

	/**
	 * Evict Artwork silently, meaning that this will not notify existing observers
	 */
	suspend fun silentEvictArtwork(id: String)
}
