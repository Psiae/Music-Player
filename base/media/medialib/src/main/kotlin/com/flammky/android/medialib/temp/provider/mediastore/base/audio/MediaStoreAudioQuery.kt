package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaStoreQuery

abstract class MediaStoreAudioQuery internal constructor() : MediaStoreQuery() {
	abstract val albumId: Long?
	abstract val artistId: Long?
	abstract val version: String
}
