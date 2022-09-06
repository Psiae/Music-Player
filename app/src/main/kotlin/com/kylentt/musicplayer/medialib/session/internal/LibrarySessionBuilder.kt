package com.kylentt.musicplayer.medialib.session.internal

import com.kylentt.musicplayer.medialib.api.player.ForwardingMediaController
import com.kylentt.musicplayer.medialib.api.player.MediaController
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.player.PlayerContext
import com.kylentt.musicplayer.medialib.session.LibrarySession
import com.kylentt.musicplayer.medialib.session.SessionContext

internal class LibrarySessionBuilder (private val context: MediaLibraryContext) {

	internal fun buildSession(builder: LibrarySession.Builder, sessionContext: SessionContext): LibrarySession {
		val playerContext = PlayerContext.Builder(context).build()
		val controller = ForwardingMediaController(playerContext)
		sessionContext.attachLibraryContext(context)
		sessionContext.attachController(controller)
		return builder.build(sessionContext)
	}

}
