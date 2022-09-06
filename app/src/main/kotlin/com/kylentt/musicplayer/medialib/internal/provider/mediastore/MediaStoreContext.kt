package com.kylentt.musicplayer.medialib.internal.provider.mediastore

import com.kylentt.musicplayer.medialib.api.provider.internal.ProvidersContext

internal class MediaStoreContext private constructor(val parent: ProvidersContext) {
	val androidContext = parent.android

	internal class Builder(private val parent: ProvidersContext) {
		fun build(): MediaStoreContext = MediaStoreContext(parent)
	}
}
