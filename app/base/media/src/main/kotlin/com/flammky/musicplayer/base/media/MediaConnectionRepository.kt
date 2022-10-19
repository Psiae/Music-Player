package com.flammky.musicplayer.base.media

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface MediaConnectionRepository {
	fun getMetadata(id: String): MediaMetadata?
	fun observeMetadata(id: String): Flow<MediaMetadata?>
	fun provideMetadata(id: String, metadata: MediaMetadata)
	fun evictMetadata(id: String)

	fun getArtwork(id: String): Any?
	fun observeArtwork(id: String): Flow<Any?>
	fun provideArtwork(id: String, artwork: Any?)
	fun evictArtwork(id: String)
}
