package com.kylentt.musicplayer.medialib.android.provider.mediastore.base

import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.audio.*

abstract class MediaStoreProviderBase {
	abstract val audioEntityProvider: AudioEntityProvider<out MediaStoreAudioEntity, out MediaStoreAudioFile, out MediaStoreAudioMetadata, out MediaStoreAudioQuery>
}
