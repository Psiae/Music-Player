package com.flammky.android.medialib.temp.session.internal

import com.flammky.android.medialib.temp.api.player.ForwardingMediaController
import com.flammky.android.medialib.temp.internal.MediaLibraryContext
import com.flammky.android.medialib.temp.player.PlayerContext
import com.flammky.android.medialib.temp.session.LibrarySession

internal class LibrarySessionBuilder (private val context: MediaLibraryContext) {

	internal fun buildSession(builder: LibrarySession.Builder, sessionContext: LibrarySessionContext): LibrarySession {
		val playerContext = PlayerContext.Builder(context).build()
		val controller = ForwardingMediaController(playerContext)
		sessionContext.attachLibraryContext(context)
		sessionContext.attachController(controller)
		return builder.build(sessionContext)
	}

}
