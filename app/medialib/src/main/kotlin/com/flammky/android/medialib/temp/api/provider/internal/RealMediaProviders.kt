package com.flammky.android.medialib.temp.api.provider.internal

import com.flammky.android.medialib.temp.api.provider.mediastore.internal.RealMediaStoreProvider
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.api.provider.MediaProviders

internal class RealMediaProviders internal constructor(
	private val context: ProvidersContext
) : MediaProviders {

	override val mediaStore by lazy {
		val ctx = MediaStoreContext.Builder(context).build()
		RealMediaStoreProvider(ctx)
	}
}
