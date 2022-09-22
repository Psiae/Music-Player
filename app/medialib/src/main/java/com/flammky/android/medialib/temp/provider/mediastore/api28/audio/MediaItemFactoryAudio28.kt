package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import android.os.Bundle
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaItemAudioFactory
import kotlin.time.Duration.Companion.milliseconds

internal class MediaItemFactoryAudio28 internal constructor(private val context: MediaStoreContext) :
	MediaItemAudioFactory<MediaStoreAudioEntity28, MediaStoreAudioFile28, MediaStoreAudioMetadata28, MediaStoreAudioQuery28>(
		context
	) {

	override fun createMetadata(
		metadataInfo: MediaStoreAudioMetadata28,
		metadataBundle: Bundle?
	): AudioMetadata {
		return AudioMetadata.build {
			setTitle(metadataInfo.title)
			setAlbumTitle(metadataInfo.album)
			setArtist(metadataInfo.artist)
			setDuration(metadataInfo.durationMs?.milliseconds)
			setPlayable((metadataInfo.durationMs ?: 0) > 0)
			if (metadataBundle != null) {
				val extra = MediaMetadata.Extra(metadataBundle)
				setExtra(extra)
			}
		}
	}
}
