package com.flammky.android.medialib.providers

import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.LibraryContext

/**
 * The Context for Providers Instances
 */
class ProvidersContext internal constructor(
	/**
	 * The Library Context this Providers belong to
	 */
	val library: LibraryContext,
	/**
	 * The Android Context that provide access to Android components such as ContentResolver
	 */
	val android: AndroidContext
)
