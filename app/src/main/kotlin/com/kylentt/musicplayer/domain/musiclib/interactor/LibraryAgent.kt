package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.dependency.Provider
import com.kylentt.musicplayer.domain.musiclib.session.LibraryPlayer
import com.kylentt.musicplayer.domain.musiclib.session.MusicSession


class LibraryAgent private constructor() {
	private lateinit var mLib: MusicLibrary

	constructor(lib: MusicLibrary) : this() {
		mLib = lib
	}

	val injector: Injector = Injector()
	val session: MusicSession = MusicSession(this)

	val mask = object : Mask {

		override val dependency: Mask.Dependency = object : Mask.Dependency {
			override fun provide(injector: Injector) = injector.fuseInjector(injector)
			override fun provide(vararg providers: Provider<Any>) = injector.addProvider(*providers)
		}

		override val session: Mask.Session = object : Mask.Session {

			override val player: LibraryPlayer
				get() = this@LibraryAgent.session.player

			override val info: MusicSession.SessionInfo
				get() = this@LibraryAgent.session.sessionInfo
		}
	}

	interface Mask {
		val dependency: Dependency
		val session: Session

		interface Dependency {
			fun provide(injector: Injector)
			fun provide(vararg providers: Provider<Any>)
		}
		interface Session {
			val player: LibraryPlayer
			val info: MusicSession.SessionInfo
		}
	}
}
