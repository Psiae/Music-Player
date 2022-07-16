package com.kylentt.musicplayer.domain.musiclib.interactor

import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.session.LibraryPlayer
import com.kylentt.musicplayer.domain.musiclib.session.MusicSession


class Agent private constructor() {
	private lateinit var mLib: MusicLibrary

	constructor(lib: MusicLibrary) : this() {
		mLib = lib
	}

	val injector: Injector = Injector()
	private val session = MusicSession(this)

	val mask: AgentMask = AgentMask(this)

	val sessionMask = object : SessionMask {

		override val player: LibraryPlayer
			get() = session.player

		override val info: MusicSession.SessionInfo
			get() = session.sessionInfo
	}

	interface SessionMask {
		val player: LibraryPlayer
		val info: MusicSession.SessionInfo
	}
}
