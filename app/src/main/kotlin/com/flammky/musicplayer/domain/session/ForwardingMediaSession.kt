package com.flammky.musicplayer.domain.session

import com.flammky.android.medialib.temp.player.LibraryPlayer

class ForwardingMediaSession() : MediaSession {

	override val player: LibraryPlayer
		get() = TODO("Not yet implemented")
}
