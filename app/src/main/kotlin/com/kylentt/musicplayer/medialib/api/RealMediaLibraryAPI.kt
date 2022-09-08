package com.kylentt.musicplayer.medialib.api

import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.api.service.ApiServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.ApiSessionComponent
import com.kylentt.musicplayer.medialib.image.ImageRepository

internal class RealMediaLibraryAPI internal constructor(internal val context: ApiContext) : MediaLibraryAPI {

	override val imageRepository: ImageRepository = context.imageRepository
	override val service: ApiServiceComponent = context.apiServiceComponent
	override val sessions: ApiSessionComponent = context.apiSessionComponent
	override val providers: MediaProviders = context.mediaProviders
}
