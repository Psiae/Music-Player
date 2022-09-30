package com.flammky.android.medialib.temp.internal

import androidx.media3.session.MediaLibraryService

internal class MediaLibraryContext(
	androidContext: AndroidContext,
	mediaLibraryServiceClass: Class<out MediaLibraryService>
) {

	val android: AndroidContext = androidContext

	val serviceClass: Class<out MediaLibraryService> = mediaLibraryServiceClass

	class Builder() {
		private var mAndroidContext: AndroidContext? = null
		private var mMediaLibraryServiceClass: Class<out MediaLibraryService>? = null

		fun setAndroidContext(androidContext: AndroidContext): Builder {
			mAndroidContext = androidContext
			return this
		}

		fun setMediaLibraryServiceClass(cls: Class<out MediaLibraryService>): Builder {
			mMediaLibraryServiceClass = cls
			return this
		}

		fun build(): MediaLibraryContext {
			return MediaLibraryContext(mAndroidContext!!, mMediaLibraryServiceClass!!)
		}
	}
}


