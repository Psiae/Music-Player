package com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.audio

import com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.media.MediaStoreQuery

abstract class MediaStoreAudioQuery internal constructor() : MediaStoreQuery() {
	abstract val albumId: Long
	abstract val artistId: Long
	abstract val version: String
}
