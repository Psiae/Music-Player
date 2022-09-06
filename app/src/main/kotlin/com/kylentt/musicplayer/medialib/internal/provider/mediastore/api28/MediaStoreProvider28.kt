package com.kylentt.musicplayer.medialib.internal.provider.mediastore.api28

import com.kylentt.musicplayer.medialib.internal.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.api28.audio.AudioEntityProvider28
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.MediaStoreProviderBase

internal class MediaStoreProvider28 internal constructor(private val context: MediaStoreContext) :
	MediaStoreProviderBase() {
	override val audioEntityProvider = AudioEntityProvider28(context)
}
