package com.kylentt.musicplayer.medialib.api.provider.mediastore

import com.kylentt.musicplayer.core.sdk.VersionHelper
import com.kylentt.musicplayer.medialib.android.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.android.provider.mediastore.api28.MediaStoreProvider28
import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.MediaStoreProviderBase
import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.audio.*

class MediaStoreProvider internal constructor(private val context: MediaStoreContext) {

	private val impl: MediaStoreProviderBase = when {
		VersionHelper.hasR() -> /* TODO */ MediaStoreProvider28(context)
		VersionHelper.hasQ() -> /* TODO */ MediaStoreProvider28(context)
		else -> /* TODO */ MediaStoreProvider28(context)
	}

	@Suppress("UNCHECKED_CAST")
	val audioProvider =
		impl.audioEntityProvider as AudioEntityProvider<MediaStoreAudioEntity, MediaStoreAudioFile, MediaStoreAudioMetadata, MediaStoreAudioQuery>
}
