package com.flammky.musicplayer.library.dump.media

import android.net.Uri
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.user.User
import dev.dexsr.klio.library.playback.PlaylistPlaybackInfo
import kotlinx.coroutines.flow.Flow

/**
 * Media Connection Interface for this library module
 */
@Deprecated("refactor (again)")
/*internal*/ interface MediaConnection {
	fun play(
		user: User,
		queue: List<Pair<String, Uri>>,
		index: Int
	)

	fun observePlaylistPlaybackInfo(
		user: User
	): Flow<PlaylistPlaybackInfo>

	val repository: Repository

	interface Repository {
		fun getArtwork(id: String): Any?
		suspend fun observeArtwork(id: String): Flow<Any?>
		suspend fun provideArtwork(id: String, artwork: Any?, silent: Boolean)
		suspend fun evictArtwork(id: String, silent: Boolean)
		fun getMetadata(id: String): MediaMetadata?
		suspend fun provideMetadata(id: String, metadata: MediaMetadata)
		suspend fun observeMetadata(id: String): Flow<MediaMetadata?>
		suspend fun evictMetadata(id: String, silent: Boolean)
	}
}
