package com.kylentt.musicplayer.medialib.internal

import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.session.internal.LibrarySessionManager

internal class MediaLibraryContext(androidContext: AndroidContext) {

	val android: AndroidContext = androidContext
	val internalSessionManager: LibrarySessionManager = LibrarySessionManager(this)

	class Builder() {
		private var mAndroidContext: AndroidContext? = null

		fun setAndroidContext(androidContext: AndroidContext): Builder {
			mAndroidContext = androidContext
			return this
		}

		fun build(): MediaLibraryContext {
			return MediaLibraryContext(mAndroidContext!!)
		}
	}
}


