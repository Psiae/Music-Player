package com.flammky.android.medialib.providers.internal

import com.flammky.android.medialib.providers.MediaProviders
import com.flammky.android.medialib.providers.ProvidersContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.internal.RealMediaStoreProvider

class RealMediaProviders(
	private val context: ProvidersContext
) : MediaProviders {

	override val mediaStore: MediaStoreProvider = RealMediaStoreProvider(MediaStoreContext(context))
}

