package com.kylentt.musicplayer.medialib.api.provider

import com.kylentt.musicplayer.medialib.api.provider.internal.ProvidersContext
import com.kylentt.musicplayer.medialib.api.provider.mediastore.MediaStoreProvider
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.MediaStoreContext

class MediaProviders internal constructor(
	private val context: ProvidersContext
) {

	val mediaStore by lazy {
		val ctx = MediaStoreContext.Builder(context).build()
		MediaStoreProvider(ctx)
	}
}
