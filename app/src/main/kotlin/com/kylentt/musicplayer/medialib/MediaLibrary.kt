package com.kylentt.musicplayer.medialib

import android.content.Context
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

class MediaLibrary private constructor(private val libContext: MediaLibraryContext) {

	private val publicAPI: MediaLibraryAPI

	init {
		construct(libContext)
		publicAPI = MediaLibraryAPI(libContext)
	}

	private fun construct(context: MediaLibraryContext) {
		// TODO
	}

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
			val libraryContext = MediaLibraryContext.Builder(context).build()
			return MediaLibrary(libraryContext)
		}
	}
}
