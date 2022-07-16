package com.kylentt.musicplayer.domain.musiclib.session

import com.kylentt.mediaplayer.core.media3.playback.PlaybackState
import com.kylentt.mediaplayer.core.media3.playback.PlaybackState.Companion.playerListener
import com.kylentt.musicplayer.domain.musiclib.interactor.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicSession(private val agent: Agent) {
	private val _playbackState = MutableStateFlow(PlaybackState.EMPTY)
	val player = LibraryPlayer(agent.injector)

	val sessionInfo = object : SessionInfo {
		override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
	}

	init {
		player.addListener(_playbackState.playerListener(
			getMediaItems = { player.getMediaItems() }
		))
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
	}
}
