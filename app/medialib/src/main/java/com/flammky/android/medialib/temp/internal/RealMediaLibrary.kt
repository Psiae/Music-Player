package com.flammky.android.medialib.temp.internal

import android.content.Context
import androidx.media3.session.MediaLibraryService
import com.flammky.common.kotlin.lazy.LazyConstructor
import com.flammky.android.medialib.temp.api.RealMediaLibraryAPI
import com.flammky.android.medialib.temp.api.internal.ApiContext
import com.flammky.android.medialib.temp.api.service.internal.InternalServiceComponent
import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionBuilder
import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionComponent
import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionManager

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

	internal companion object {
		private val lazyConstructor: LazyConstructor<RealMediaLibrary> = LazyConstructor()
		private val instance by lazyConstructor

		internal val API: RealMediaLibraryAPI
			get() = instance.publicAPI

		internal fun isConstructedAtomic() = lazyConstructor.isConstructedAtomic()

		internal fun construct(context: Context, cls: Class<out MediaLibraryService>): RealMediaLibraryAPI {
			lazyConstructor.construct { createLibrary(context, cls) }
			return instance.publicAPI
		}

		private fun createLibrary(context: Context, cls: Class<out MediaLibraryService>): RealMediaLibrary {
			val androidContext = AndroidContext(context)
			val libContext = MediaLibraryContext.Builder()
				.setAndroidContext(androidContext)
				.setMediaLibraryServiceClass(cls)
				.build()
			return RealMediaLibrary(libContext)
		}
	}
}
