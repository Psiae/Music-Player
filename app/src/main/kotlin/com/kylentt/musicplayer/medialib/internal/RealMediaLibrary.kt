package com.kylentt.musicplayer.medialib.internal

import android.content.Context
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.api.RealMediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.service.internal.InternalServiceComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionBuilder
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionComponent
import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionManager

internal class RealMediaLibrary private constructor(private val context: MediaLibraryContext) {

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

	private val publicAPI: RealMediaLibraryAPI = RealMediaLibraryAPI(publicApiContext)

	companion object {
		private val lazyConstructor: LazyConstructor<RealMediaLibrary> = LazyConstructor()
		private val instance by lazyConstructor

		internal val API: RealMediaLibraryAPI
			get() = instance.publicAPI

		internal fun isConstructedAtomic() = lazyConstructor.isConstructedAtomic()

		fun construct(context: Context): RealMediaLibraryAPI {
			lazyConstructor.construct { createLibrary(context) }
			return instance.publicAPI
		}

		private fun createLibrary(context: Context): RealMediaLibrary {
			val androidContext = AndroidContext(context)
			val libContext = MediaLibraryContext.Builder().setAndroidContext(androidContext).build()
			return RealMediaLibrary(libContext)
		}
	}
}
