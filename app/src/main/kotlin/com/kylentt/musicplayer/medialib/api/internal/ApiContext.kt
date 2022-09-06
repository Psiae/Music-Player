package com.kylentt.musicplayer.medialib.api.internal

import com.kylentt.musicplayer.medialib.android.internal.AndroidContext
import com.kylentt.musicplayer.medialib.api.session.ApiSessionManager
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

internal class ApiContext(
	val libraryContext: MediaLibraryContext,
	val sessionManager: ApiSessionManager
) {

	val android: AndroidContext = libraryContext.android

	class Builder() {
		private var mLibraryContext: MediaLibraryContext? = null
		private var mSessionManager: ApiSessionManager? = null

		fun setLibraryContext(context: MediaLibraryContext): Builder {
			mLibraryContext = context
			return this
		}

		fun setSessionManager(sessionManager: ApiSessionManager): Builder {
			mSessionManager = sessionManager
			return this
		}


		fun build(): ApiContext {
			return ApiContext(mLibraryContext!!, mSessionManager!!)
		}
	}
}
