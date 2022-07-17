package com.kylentt.musicplayer.domain.musiclib

import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent

class MusicLibrary private constructor() {
	val agent = LibraryAgent(this)
	companion object {
		private val musicLibrary = MusicLibrary()

		// Singleton Interactor, hide whatever mess happen behind
		val localAgent: LibraryAgent.Mask
			get() = musicLibrary.agent.mask
	}
}
