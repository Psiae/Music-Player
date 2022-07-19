package com.kylentt.musicplayer.domain.musiclib.session

import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent
import kotlinx.coroutines.flow.StateFlow

class MusicSession(private val agent: LibraryAgent) {
	val player = LibraryPlayer(agent)

	val sessionInfo = object : SessionInfo {
		override val playbackState: StateFlow<PlaybackState> = PlaybackState.StateFlow(player)
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
	}
}
