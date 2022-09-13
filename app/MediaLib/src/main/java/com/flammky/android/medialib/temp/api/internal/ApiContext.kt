package com.flammky.android.medialib.temp.api.internal

import com.flammky.android.medialib.temp.internal.AndroidContext
import com.flammky.android.medialib.temp.api.provider.internal.RealMediaProviders
import com.flammky.android.medialib.temp.api.provider.internal.ProvidersContext
import com.flammky.android.medialib.temp.api.service.ApiServiceComponent
import com.flammky.android.medialib.temp.api.service.internal.InternalServiceComponent
import com.flammky.android.medialib.temp.api.session.component.internal.ApiSessionComponent
import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionComponent
import com.flammky.android.medialib.temp.image.ImageRepository
import com.flammky.android.medialib.temp.image.internal.RealImageRepository
import com.flammky.android.medialib.temp.internal.MediaLibraryContext

internal class ApiContext private constructor(
    val libraryContext: MediaLibraryContext,
    val internalSessionComponent: InternalSessionComponent,
    val internalServiceComponent: InternalServiceComponent
) {

	val android: AndroidContext = libraryContext.android

	val apiServiceComponent = ApiServiceComponent(internalServiceComponent)
	val apiSessionComponent = ApiSessionComponent(internalSessionComponent)

	val imageRepository: ImageRepository = RealImageRepository(android)

	val mediaProviders: RealMediaProviders = RealMediaProviders(ProvidersContext(android))

	class Builder() {
		private var mLibraryContext: MediaLibraryContext? = null
		private var mServiceComponent: InternalServiceComponent? = null
		private var mSessionComponent: InternalSessionComponent? = null

		val libraryContext
			get() = mLibraryContext

		val serviceComponent
			get() = mServiceComponent

		val sessionComponent
			get() = mSessionComponent

		fun setLibraryContext(context: MediaLibraryContext): Builder {
			mLibraryContext = context
			return this
		}

		fun setServiceComponent(serviceComponent: InternalServiceComponent): Builder {
			mServiceComponent = serviceComponent
			return this
		}

		fun setSessionComponent(sessionComponent: InternalSessionComponent): Builder {
			mSessionComponent = sessionComponent
			return this
		}

		fun build(): ApiContext {
			return ApiContext(libraryContext!!, sessionComponent!!, serviceComponent!!)
		}
	}
}
