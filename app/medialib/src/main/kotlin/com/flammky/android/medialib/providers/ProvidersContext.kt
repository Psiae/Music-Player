package com.flammky.android.medialib.providers

import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.LibraryContext

/**
 * Context for Providers Instances
 */
abstract class ProvidersContext internal constructor() {

	/**
	 * The Library Context this Providers belong to
	 */
	abstract val library: LibraryContext

	/**
	 * The Android Context that provide access to external components such as ContentResolver
	 */
	abstract val android: AndroidContext
}
