package com.kylentt.musicplayer.medialib.api

import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

class MediaLibraryAPI internal constructor(
	private val context: MediaLibraryContext
) {

	val providers: MediaProviders = MediaProviders(context)


	companion object {
		val current: MediaLibraryAPI? get() = MediaLibrary.API
	}
}
