package com.kylentt.musicplayer.medialib.api

import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.api.service.ApiServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.ApiSessionComponent
import com.kylentt.musicplayer.medialib.image.ImageRepository

interface MediaLibraryAPI {
	val imageRepository: ImageRepository
	val service: ApiServiceComponent
	val sessions: ApiSessionComponent
	val providers: MediaProviders
}
