package com.flammky.android.medialib.temp.api

import com.flammky.android.medialib.temp.api.internal.ApiContext
import com.flammky.android.medialib.temp.api.provider.MediaProviders
import com.flammky.android.medialib.temp.api.service.ApiServiceComponent
import com.flammky.android.medialib.temp.api.session.component.internal.ApiSessionComponent
import com.flammky.android.medialib.temp.image.ImageRepository

internal class RealMediaLibraryAPI internal constructor(internal val context: ApiContext) :
	MediaLibraryAPI {

	override val imageRepository: ImageRepository = context.imageRepository
	override val service: ApiServiceComponent = context.apiServiceComponent
	override val sessions: ApiSessionComponent = context.apiSessionComponent
	override val providers: MediaProviders = context.mediaProviders
}
