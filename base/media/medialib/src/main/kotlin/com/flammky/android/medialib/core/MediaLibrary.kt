package com.flammky.android.medialib.core

import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.providers.MediaProviders

abstract class MediaLibrary internal constructor(
	val context: LibraryContext
) {


	abstract val mediaProviders: MediaProviders
}
