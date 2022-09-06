package com.kylentt.musicplayer.medialib.internal.provider.mediastore

import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

class MediaStoreContext private constructor(val libContext: MediaLibraryContext) {
	val androidContext = libContext.android

	internal class Builder(val libContext: MediaLibraryContext) {
		fun build(): MediaStoreContext = MediaStoreContext(libContext)
	}
}
