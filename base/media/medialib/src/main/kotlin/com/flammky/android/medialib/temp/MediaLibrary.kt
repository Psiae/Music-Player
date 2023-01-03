package com.flammky.android.medialib.temp

import android.content.Context
import androidx.media3.session.MediaLibraryService
import com.flammky.android.medialib.temp.api.MediaLibraryAPI
import com.flammky.android.medialib.temp.internal.RealMediaLibrary

@Deprecated("")
object MediaLibrary {

	private val delegate = RealMediaLibrary

	/**
	 * Singleton API
	 */
	val API: MediaLibraryAPI
		get() {
			require(delegate.isConstructedAtomic()) {
				"MediaLibrary was not constructed"
			}
			return delegate.API
		}

	fun construct(context: Context, cls: Class<out MediaLibraryService>): MediaLibraryAPI = delegate.construct(context, cls)
}
