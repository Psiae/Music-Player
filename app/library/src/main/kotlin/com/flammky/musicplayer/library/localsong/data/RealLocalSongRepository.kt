package com.flammky.musicplayer.library.localsong.data

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.milliseconds

class RealLocalSongRepository(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers
) : LocalSongRepository {
	private val mediaLib = MediaLib.singleton(context)
	private val audioProvider = mediaLib.mediaProviders.mediaStore.audio


	override suspend fun getEntitiesAsync(cache: Boolean): Deferred<List<LocalSongEntity>> =
		coroutineScope {
			async(dispatchers.io) {
				audioProvider.query().map { toLocalSongEntity(it) }
			}
		}

	private fun toLocalSongEntity(from: MediaStoreAudioEntity): LocalSongEntity {
		val metadata = AudioMetadata.build {
			val durationMs = from.metadata.durationMs
			setArtist(from.metadata.artist)
			setAlbumTitle(from.metadata.album)
			setTitle(from.metadata.title)
			setPlayable(if (durationMs != null && durationMs > 0) true else null)
			setDuration(durationMs?.milliseconds)
		}

		val mediaItem = mediaLib.context.buildMediaItem {
			setMediaId(from.uid)
			setMediaUri(from.uri)
			setExtra(MediaItem.Extra())
			setMetadata(metadata)
		}

		return MediaStoreLocalSongEntity(from, mediaItem)
	}

	class MediaStoreLocalSongEntity(
		val mediaStore: MediaStoreAudioEntity,
		mediaItem: MediaItem
	) : LocalSongEntity(mediaStore.uid, mediaItem) {

	}
}
