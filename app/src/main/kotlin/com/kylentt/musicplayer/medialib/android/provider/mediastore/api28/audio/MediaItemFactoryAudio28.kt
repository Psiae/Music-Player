package com.kylentt.musicplayer.medialib.android.provider.mediastore.api28.audio

import android.os.Bundle
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.medialib.android.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.media.MediaItemFactory

internal class MediaItemFactoryAudio28 internal constructor(private val context: MediaStoreContext) :
	MediaItemFactory<MediaStoreAudioEntity28, MediaStoreAudioFile28, MediaStoreAudioMetadata28, MediaStoreAudioQuery28>(
		context
	) {

	override fun fillMetadata(
		metadataInfo: MediaStoreAudioMetadata28,
		mediaMetadata: MediaMetadata.Builder,
		extra: Bundle
	) {
		super.fillMetadata(metadataInfo, mediaMetadata, extra)
		mediaMetadata.apply {
			setAlbumTitle(metadataInfo.album)
			setArtist(metadataInfo.artist)
			setComposer(metadataInfo.composer)
			setSubtitle(metadataInfo.artist)
			setIsPlayable(metadataInfo.durationMs > 0)
			setRecordingYear(metadataInfo.year)
		}
	}

}
