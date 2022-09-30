package com.flammky.android.medialib.temp.api.session.component.internal

import com.flammky.android.medialib.temp.api.session.component.SessionBuilder
import com.flammky.android.medialib.temp.api.player.ForwardingMediaController
import com.flammky.android.medialib.temp.internal.MediaLibraryContext
import com.flammky.android.medialib.temp.player.PlayerContext
import com.flammky.android.medialib.temp.session.LibrarySession
import com.flammky.android.medialib.temp.session.internal.LibrarySessionContext

internal class InternalSessionBuilder(private val context: MediaLibraryContext) : SessionBuilder {

	override fun buildSession(
		fill: LibrarySession.Builder.() -> Unit
	): LibrarySession {
		val filledBuilder = LibrarySession.Builder().apply { fill() }.buildUpon()
		val sessionContext = createSessionContext(filledBuilder)
		return filledBuilder.build(sessionContext)
	}

	private fun createSessionContext(sessionBuilder: LibrarySession.Builder): LibrarySessionContext {
		val playerContext = PlayerContext.Builder(context).build()
		return LibrarySessionContext.Builder(sessionBuilder.id).build()
			.apply {
				attachLibraryContext(context)
				attachController(ForwardingMediaController(playerContext))
			}
	}

}
