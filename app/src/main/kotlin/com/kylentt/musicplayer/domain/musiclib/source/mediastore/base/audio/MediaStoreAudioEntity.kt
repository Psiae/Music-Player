package com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.audio

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.media.MediaStoreEntity

abstract class MediaStoreAudioEntity : MediaStoreEntity() {
	abstract override val fileInfo: MediaStoreAudioFile
	abstract override val metadata: MediaStoreAudioMetadata
}
