package com.flammky.android.medialib.temp.session.internal

import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.util.Assertions.checkMainThread
import com.flammky.android.medialib.temp.api.player.MediaController
import com.flammky.android.medialib.temp.internal.MediaLibraryContext

internal class LibrarySessionContext private constructor(val id: String) {
	private var _LibraryContext: MediaLibraryContext? = null
	private var _MediaController: MediaController? = null

	@MainThread
	internal fun attachLibraryContext(context: MediaLibraryContext) {
		checkThreadAccess()
		check(_LibraryContext == null)
		_LibraryContext = context
	}

	@MainThread
	internal fun attachController(controller: MediaController) {
		checkThreadAccess()
		check(_MediaController == null)
		_MediaController = controller
	}

	internal val controller: MediaController
		get() = _MediaController!!

	private fun checkThreadAccess() {
		check(Looper.myLooper() == Looper.getMainLooper())
	}

	class Builder(val id: String) {
		fun build(): LibrarySessionContext = LibrarySessionContext(id)
	}
}
