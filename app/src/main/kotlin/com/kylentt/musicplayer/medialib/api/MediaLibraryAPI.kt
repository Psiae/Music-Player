package com.kylentt.musicplayer.medialib.api

import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.api.provider.internal.ProvidersContext

class MediaLibraryAPI internal constructor(internal val context: ApiContext) {

	val sessionManager = context.sessionManager

	val providers: MediaProviders = MediaProviders(ProvidersContext(context.android))

	companion object {
		val current: MediaLibraryAPI? get() = MediaLibrary.API
	}
}
