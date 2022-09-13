package com.kylentt.musicplayer.domain.session

class ForwardingMediaSession() : MediaSession {

	override val player: com.flammky.android.medialib.temp.player.LibraryPlayer
		get() = TODO("Not yet implemented")
}
