package com.kylentt.musicplayer.medialib.api.internal

import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.api.provider.MediaProviders
import com.kylentt.musicplayer.medialib.api.provider.internal.ProvidersContext
import com.kylentt.musicplayer.medialib.api.service.ApiServiceComponent
import com.kylentt.musicplayer.medialib.api.service.internal.InternalServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.ApiSessionComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionComponent
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

internal class ApiContext private constructor(
	val libraryContext: MediaLibraryContext,
	val internalSessionComponent: InternalSessionComponent,
	val internalServiceComponent: InternalServiceComponent
) {

	val android: AndroidContext = libraryContext.android

	val apiServiceComponent = ApiServiceComponent(internalServiceComponent)
	val apiSessionComponent = ApiSessionComponent(internalSessionComponent)

	val mediaProviders: MediaProviders = MediaProviders(ProvidersContext(android))

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
