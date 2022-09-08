package com.kylentt.musicplayer.medialib.android.provider.mediastore.api28

import com.kylentt.musicplayer.medialib.android.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.android.provider.mediastore.api28.audio.AudioEntityProvider28
import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.MediaStoreProviderBase

internal class MediaStoreProvider28 internal constructor(private val context: MediaStoreContext) :
	MediaStoreProviderBase() {
	override val audioEntityProvider = AudioEntityProvider28(context)
}
