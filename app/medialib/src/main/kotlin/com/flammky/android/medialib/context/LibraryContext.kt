package com.flammky.android.medialib.context

import com.flammky.android.medialib.context.internal.InternalLibraryContext
import com.flammky.android.medialib.core.MediaLibrary

/**
 * The Context for [MediaLibrary] Instances.
 *
 * Implementations are internal, but Configurable
 */

abstract class LibraryContext internal constructor() {

	abstract val android: AndroidContext

	internal abstract val internal: InternalLibraryContext
}
