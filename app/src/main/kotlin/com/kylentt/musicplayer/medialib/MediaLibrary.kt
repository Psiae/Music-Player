package com.kylentt.musicplayer.medialib

import android.content.Context
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.service.internal.InternalServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionBuilder
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionManager
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

class MediaLibrary private constructor(private val context: MediaLibraryContext) {

	private val internalSessionBuilder = InternalSessionBuilder(context)
	private val internalSessionManager = InternalSessionManager(context)
	private val internalSessionComponent = InternalSessionComponent(internalSessionBuilder, internalSessionManager, context)
	private val internalServiceComponent = InternalServiceComponent(context)

	private val publicApiContext = ApiContext.Builder()
		.apply {
			setSessionComponent(internalSessionComponent)
			setServiceComponent(internalServiceComponent)
			setLibraryContext(context)
		}
		.build()

	private val publicAPI: MediaLibraryAPI = MediaLibraryAPI(publicApiContext)

	companion object {
		private val lazyConstructor: LazyConstructor<MediaLibrary> = LazyConstructor()
		private val instance by lazyConstructor

		val API: MediaLibraryAPI?
			get() = lazyConstructor.valueOrNull?.publicAPI

		fun construct(context: Context): MediaLibraryAPI {
			lazyConstructor.construct { createLibrary(context) }
			return instance.publicAPI
		}

		private fun createLibrary(context: Context): MediaLibrary {
			val androidContext = AndroidContext(context)
			val libContext = MediaLibraryContext.Builder().setAndroidContext(androidContext).build()
			return MediaLibrary(libContext)
		}
	}
}
