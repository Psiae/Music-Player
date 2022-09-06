package com.kylentt.musicplayer.medialib.api.session.component.internal

import com.kylentt.musicplayer.medialib.api.session.component.SessionBuilder
import com.kylentt.musicplayer.medialib.api.player.ForwardingMediaController
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.player.PlayerContext
import com.kylentt.musicplayer.medialib.session.LibrarySession
import com.kylentt.musicplayer.medialib.session.SessionContext

internal class InternalSessionBuilder(private val context: MediaLibraryContext) : SessionBuilder {

	override fun buildSession(
		fill: LibrarySession.Builder.() -> Unit
	): LibrarySession {
		val filledBuilder = LibrarySession.Builder().apply { fill() }.buildUpon()
		val sessionContext = createSessionContext(filledBuilder)
		return filledBuilder.build(sessionContext)
	}

	private fun createSessionContext(sessionBuilder: LibrarySession.Builder): SessionContext {
		val playerContext = PlayerContext.Builder(context).build()
		return SessionContext.Builder(sessionBuilder.id).build()
			.apply {
				attachLibraryContext(context)
				attachController(ForwardingMediaController(playerContext))
			}
	}

}
