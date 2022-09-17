package com.flammky.android.medialib.temp.provider.mediastore

import com.flammky.android.medialib.temp.api.provider.internal.ProvidersContext

internal class MediaStoreContext private constructor(val parent: ProvidersContext) {
	val androidContext = parent.android

	val eventDispatcher
		get() = parent.eventDispatcher

	internal class Builder(private val parent: ProvidersContext) {
		fun build(): MediaStoreContext = MediaStoreContext(parent)
	}
}
