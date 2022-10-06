package com.flammky.android.medialib.providers

import com.flammky.android.medialib.context.LibraryContext

/**
 * The Context for Providers Instances
 */
class ProvidersContext internal constructor(

	/**
	 * The Library Context this Providers belong to
	 */
	val library: LibraryContext,
) {

	companion object {
		inline val ProvidersContext.android
			get() = library.android
	}
}
