package com.kylentt.musicplayer.medialib

import android.content.Context
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.internal.RealMediaLibrary

object MediaLibrary {

	private val delegate = RealMediaLibrary

	val API: MediaLibraryAPI
		get() {
			require(delegate.isConstructedAtomic()) {
				"MediaLibrary was not constructed"
			}
			return delegate.API
		}

	fun construct(context: Context): MediaLibraryAPI = RealMediaLibrary.construct(context)
}
