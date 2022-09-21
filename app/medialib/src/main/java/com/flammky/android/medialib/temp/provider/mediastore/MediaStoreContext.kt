package com.flammky.android.medialib.temp.provider.mediastore

import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.temp.api.provider.internal.ProvidersContext

internal class MediaStoreContext private constructor(private val parent: ProvidersContext) {
	val androidContext
		get() = parent.android

	val eventDispatcher
		get() = parent.eventDispatcher

	val library: LibraryContext
		get() = parent.library

	internal class Builder(private val parent: ProvidersContext) {
		fun build(): MediaStoreContext = MediaStoreContext(parent)
	}
}
