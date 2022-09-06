package com.kylentt.musicplayer.domain.musiclib.interactor

import android.content.Context
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.dependency.Provider
import com.kylentt.musicplayer.domain.musiclib.session.MusicSession
import com.kylentt.musicplayer.medialib.player.LibraryPlayer

@Deprecated("TODO")
class LibraryAgent(val context: Context)  {

	val mask = object : Mask {

		override fun connect(): Mask {
			return this
		}

		override val dependency: Mask.Dependency = object : Mask.Dependency {
			override fun provide(injector: Injector) = injector.fuse(injector)
			override fun provide(vararg providers: Provider<Any>) = injector.addProvider(*providers)
		}

		override val session: Mask.Session = object : Mask.Session {

			override val player: LibraryPlayer
				get() = this@LibraryAgent.session.player

			override val info: MusicSession.SessionInfo
				get() = this@LibraryAgent.session.sessionInfo
		}
	}

	val injector: Injector = Injector()
	val session: MusicSession = MusicSession(this)

	interface Mask {
		val dependency: Dependency
		val session: Session

		fun connect(): Mask

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
