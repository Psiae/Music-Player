package com.kylentt.musicplayer.medialib.api.provider

import com.kylentt.musicplayer.medialib.api.provider.internal.ProvidersContext
import com.kylentt.musicplayer.medialib.api.provider.mediastore.MediaStoreProvider
import com.kylentt.musicplayer.medialib.android.provider.mediastore.MediaStoreContext

internal class RealMediaProviders internal constructor(
	private val context: ProvidersContext
) : MediaProviders {

	override val mediaStore by lazy {
		val ctx = MediaStoreContext.Builder(context).build()
		MediaStoreProvider(ctx)
	}
}
