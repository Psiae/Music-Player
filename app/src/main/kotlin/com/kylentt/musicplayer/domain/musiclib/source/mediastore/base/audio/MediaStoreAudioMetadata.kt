package com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.audio

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.media.MediaStoreMetadata

abstract class MediaStoreAudioMetadata : MediaStoreMetadata() {

	abstract val albumName: String

	abstract val albumId: Long

	abstract val artistName: String

	abstract val artistId: Long

}
