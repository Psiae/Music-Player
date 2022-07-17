package com.kylentt.musicplayer.domain.musiclib.session

import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState
import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState.Companion.byPlayerListener
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicSession(private val agent: LibraryAgent) {
	val player = LibraryPlayer(agent.injector)

	val sessionInfo = object : SessionInfo {
		private val mPlaybackState = MutableStateFlow(PlaybackState.EMPTY)
		private val mPlaybackListener = mPlaybackState.byPlayerListener(
			getMediaItems = { player.getMediaItems() }
		)

		init {
			player.addListener(mPlaybackListener)
		}

		override val playbackState: StateFlow<PlaybackState> = mPlaybackState.asStateFlow()
	}

	interface SessionInfo {
		val playbackState: StateFlow<PlaybackState>
	}
}
