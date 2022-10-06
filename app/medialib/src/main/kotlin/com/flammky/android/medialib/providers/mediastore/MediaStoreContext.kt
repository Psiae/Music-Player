package com.flammky.android.medialib.providers.mediastore

import com.flammky.android.medialib.providers.ProvidersContext
import com.flammky.android.medialib.providers.ProvidersContext.Companion.android

/**
 * The Context for Media Store Provider instance
 */
class MediaStoreContext internal constructor(
	/** Parent ProviderContext */
	val parent: ProvidersContext
) {
	companion object {
		inline val MediaStoreContext.android
			get() = parent.android
		inline val MediaStoreContext.library
			get() = parent.library
	}
}
