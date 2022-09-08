package com.kylentt.musicplayer.medialib.android.provider.mediastore.base.audio

import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.media.MediaStoreEntity

abstract class MediaStoreAudioEntity internal constructor() : MediaStoreEntity() {
	abstract override val fileInfo: MediaStoreAudioFile
	abstract override val metadataInfo: MediaStoreAudioMetadata
}
