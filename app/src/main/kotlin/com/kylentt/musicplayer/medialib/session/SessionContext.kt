package com.kylentt.musicplayer.medialib.session

import androidx.annotation.MainThread
import com.kylentt.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread
import com.kylentt.musicplayer.medialib.api.player.MediaController
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

internal class SessionContext private constructor(val id: String) {
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
		checkMainThread() {
			"Invalid Internal Usage, SessionContext was accessed on the wrong thread"
		}
	}

	class Builder(val id: String) {
		fun build(): SessionContext = SessionContext(id)
	}
}
