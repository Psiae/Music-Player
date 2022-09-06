package com.kylentt.musicplayer.medialib

import android.content.Context
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.internal.ApiContext
import com.kylentt.musicplayer.medialib.api.session.ApiSessionManager
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.session.internal.LibrarySessionManager

class MediaLibrary private constructor(private val context: MediaLibraryContext) {

	private val apiSessionManager = ApiSessionManager(context.internalSessionManager)
	private val publicAPI: MediaLibraryAPI = MediaLibraryAPI(ApiContext(context, apiSessionManager))

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
