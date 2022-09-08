package com.kylentt.musicplayer.medialib.api

import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.api.service.ApiServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.ApiSessionComponent
import com.kylentt.musicplayer.medialib.image.ImageManager

class MediaLibraryAPI internal constructor(internal val context: ApiContext) {

	val imageManager: ImageManager = context.imageManager
	val serviceComponent: ApiServiceComponent = context.apiServiceComponent
	val sessions: ApiSessionComponent = context.apiSessionComponent
	val providers: MediaProviders = context.mediaProviders

	companion object {
		val current: MediaLibraryAPI? get() = MediaLibrary.API
	}
}
