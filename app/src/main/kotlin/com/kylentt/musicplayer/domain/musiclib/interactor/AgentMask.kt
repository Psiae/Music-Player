package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.dependency.Provider
import com.kylentt.musicplayer.domain.musiclib.session.LibraryPlayer
import com.kylentt.musicplayer.domain.musiclib.session.MusicSession

class AgentMask private constructor() {
	private lateinit var mAgent: Agent

	constructor(agent: Agent) : this() {
		mAgent = agent
	}

	val dependency = object : Dependency {
		override fun provide(injector: Injector): Unit = mAgent.injector.fuseInjector(injector)
		override fun provide(vararg providers: Provider<Any>) = mAgent.injector.addProvider(*providers)
	}

	val session = object : Session {
		override val controller: LibraryPlayer
			get() = mAgent.sessionMask.player
		override val info: MusicSession.SessionInfo
			get() = mAgent.sessionMask.info
	}

	interface Dependency {
		fun provide(injector: Injector)
		fun provide(vararg providers: Provider<Any>)
	}

	interface Session {
		val controller: LibraryPlayer
		val info: MusicSession.SessionInfo
	}
}
