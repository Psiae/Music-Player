package com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.entity.MediaStoreEntity

abstract class MediaStoreAudioEntity internal constructor() : MediaStoreEntity() {
	abstract override val fileInfo: MediaStoreAudioFile
	abstract override val metadataInfo: MediaStoreAudioMetadata
}
