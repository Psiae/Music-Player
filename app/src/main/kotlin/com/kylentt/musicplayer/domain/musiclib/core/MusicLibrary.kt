package com.kylentt.musicplayer.domain.musiclib.core

import android.content.Context
import com.kylentt.musicplayer.common.late.LateLazy
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent

class MusicLibrary private constructor(
	val context: Context
) {

































	val agent = LibraryAgent(this)
	companion object {
		private val constructor = LateLazy<MusicLibrary>()
		private val musicLibrary by constructor

		fun construct(context: Context): Unit {
			constructor.construct { MusicLibrary(context) }
		}

		// Singleton Interactor, hide whatever mess happen behind
		val localAgent: LibraryAgent.Mask
			get() = musicLibrary.agent.mask
	}
}
