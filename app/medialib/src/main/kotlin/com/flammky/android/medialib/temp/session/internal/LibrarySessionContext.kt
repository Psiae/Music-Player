package com.flammky.android.medialib.temp.session.internal

import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.android.medialib.temp.api.player.MediaController
import com.flammky.android.medialib.temp.internal.MediaLibraryContext

internal class LibrarySessionContext private constructor(val id: String) {
	private var _libraryContext: MediaLibraryContext? = null
	private var _mediaController: MediaController? = null

	@MainThread
	internal fun attachLibraryContext(context: MediaLibraryContext) {
		checkThreadAccess()
		check(_libraryContext == null)
		_libraryContext = context
	}

	@MainThread
	internal fun attachController(controller: MediaController) {
		checkThreadAccess()
		check(_mediaController == null)
		_mediaController = controller
	}

	internal val controller: MediaController
		get() = _mediaController!!

	private fun checkThreadAccess() {
		check(Looper.myLooper() == Looper.getMainLooper())
	}

	class Builder(val id: String) {
		fun build(): LibrarySessionContext = LibrarySessionContext(id)
	}
}
