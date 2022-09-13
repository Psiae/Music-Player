package com.flammky.android.medialib.temp.api.provider.mediastore

import com.flammky.android.medialib.temp.media3.contract.MediaItemFactoryOf
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.*

interface MediaStoreProvider {
	val audio: Audio


	interface Audio {
		val mediaItemFactory: MediaItemFactoryOf<MediaStoreAudioEntity>
		suspend fun query(): List<MediaStoreAudioEntity>
	}
}
