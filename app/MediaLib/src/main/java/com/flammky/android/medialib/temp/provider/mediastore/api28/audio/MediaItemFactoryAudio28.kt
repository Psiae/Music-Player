package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import android.os.Bundle
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaItemAudioFactory

internal class MediaItemFactoryAudio28 internal constructor(private val context: MediaStoreContext) :
	MediaItemAudioFactory<MediaStoreAudioEntity28, MediaStoreAudioFile28, MediaStoreAudioMetadata28, MediaStoreAudioQuery28>(
		context
	) {

	override fun createMediaItem(
		source: MediaStoreAudioEntity28,
		itemBundle: Bundle?,
		metadataBundle: Bundle?
	): MediaItem {
		val metadata = createMetadata(source.metadataInfo, metadataBundle)
		return MediaItem.Builder(source.uid, source.uri)
			.apply {
				setMetadata(metadata)
				if (itemBundle != null) setBundle(itemBundle)
			}
			.build()
	}

	override fun createMetadata(
		metadataInfo: MediaStoreAudioMetadata28,
		bundle: Bundle?
	): AudioMetadata {
		return AudioMetadata.Builder()
			.apply {
				setTitle(metadataInfo.title)
				setAlbum(metadataInfo.album)
				setArtist(metadataInfo.artist)
				setDuration(metadataInfo.durationMs)
				setPlayable((metadataInfo.durationMs ?: 0) > 0)
				if (bundle != null) setBundle(bundle)
			}
			.build()
	}

}
