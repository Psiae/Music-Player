package com.flammky.android.medialib.context

import com.flammky.android.medialib.core.MediaLibrary

/**
 * The Context for [MediaLibrary] Instances.
 *
 * Implementations are internal, but Configurable
 */

abstract class LibraryContext internal constructor(
	val android: AndroidContext,
) {

}
