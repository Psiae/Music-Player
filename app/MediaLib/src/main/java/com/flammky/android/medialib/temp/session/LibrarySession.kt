package com.flammky.android.medialib.temp.session

import com.flammky.android.medialib.temp.api.player.MediaController
import com.flammky.android.medialib.temp.session.internal.LibrarySessionContext

class LibrarySession private constructor(private val context: LibrarySessionContext) {

	val id: String = context.id

	val mediaController: MediaController get() = context.controller

	class Builder internal constructor() {
		private var mId: String = ""

		val id: String
			get() = mId

		fun setId(id: String): Builder {
			mId = id
			return this
		}

		internal fun build(context: LibrarySessionContext): LibrarySession {
			return LibrarySession(context)
		}

		fun buildUpon(): Builder {
			val id = id

			return Builder()
				.apply {
					setId(id)
				}
		}
	}
}
