package com.kylentt.musicplayer.domain.musiclib

import com.kylentt.musicplayer.domain.musiclib.interactor.Agent
import com.kylentt.musicplayer.domain.musiclib.interactor.AgentMask

class MusicLibrary private constructor() {

	private val agent = Agent(this)

	companion object {
		private val musicLibrary = MusicLibrary()

		// Singleton Interactor
		val localAgent: AgentMask = musicLibrary.agent.mask
	}
}
