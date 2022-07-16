package com.kylentt.musicplayer.domain.musiclib

import android.os.Handler
import android.os.Looper
import com.kylentt.mediaplayer.core.extenstions.sync
import com.kylentt.musicplayer.domain.musiclib.interactor.Agent
import com.kylentt.musicplayer.domain.musiclib.interactor.AgentMask

class MusicLibrary private constructor() {
	val agent = Agent(this)

	companion object {
		private val musicLibrary = MusicLibrary()

		// Singleton Interactor, hide whatever mess happen behind
		val localAgent: AgentMask
			get() = musicLibrary.agent.mask
	}
}
