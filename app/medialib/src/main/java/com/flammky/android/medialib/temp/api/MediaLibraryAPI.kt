package com.flammky.android.medialib.temp.api

import com.flammky.android.medialib.temp.api.provider.MediaProviders
import com.flammky.android.medialib.temp.api.service.ApiServiceComponent
import com.flammky.android.medialib.temp.api.session.component.internal.ApiSessionComponent
import com.flammky.android.medialib.temp.image.ImageRepository

interface MediaLibraryAPI {
	val imageRepository: ImageRepository
	val service: ApiServiceComponent
	val sessions: ApiSessionComponent
	val providers: MediaProviders
}
