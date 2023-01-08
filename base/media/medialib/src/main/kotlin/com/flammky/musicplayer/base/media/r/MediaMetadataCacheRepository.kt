package com.flammky.musicplayer.base.media.r

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface MediaMetadataCacheRepository {
	fun getMetadata(id: String): MediaMetadata?
	fun observeMetadata(id: String): Flow<MediaMetadata?>
	fun provideMetadata(id: String, metadata: MediaMetadata)
	fun evictMetadata(id: String)

	/**
	 * Provide Metadata silently, meaning that this will not notify existing observers
	 */
	fun silentProvideMetadata(id: String, metadata: MediaMetadata)

	/**
	 * Evict Metadata silently, meaning that this will not notify existing observers
	 */
	fun silentEvictMetadata(id: String)

	fun getArtwork(id: String): Any?
	fun observeArtwork(id: String): Flow<Any?>
	fun provideArtwork(id: String, artwork: Any)
	fun evictArtwork(id: String)

	/**
	 * Provide Artwork silently, meaning that this will not notify existing observers
	 */
	fun silentProvideArtwork(id: String, artwork: Any)

	/**
	 * Evict Artwork silently, meaning that this will not notify existing observers
	 */
	fun silentEvictArtwork(id: String)
}
