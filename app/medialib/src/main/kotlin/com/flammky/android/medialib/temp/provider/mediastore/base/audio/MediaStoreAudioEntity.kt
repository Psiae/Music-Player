package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaStoreEntity

abstract class MediaStoreAudioEntity internal constructor() : MediaStoreEntity() {
	abstract override val fileInfo: MediaStoreAudioFile
	abstract override val metadataInfo: MediaStoreAudioMetadata
}
