package com.flammky.musicplayer.domain.musiclib.session

import androidx.media3.common.MediaItem
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState
import com.flammky.musicplayer.domain.musiclib.interactor.LibraryAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MusicSession(private val agent: LibraryAgent) {

	val player: com.flammky.android.medialib.temp.api.player.MediaController = com.flammky.android.medialib.temp.MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!.mediaController

	val sessionInfo = object : SessionInfo {
		private val scope = CoroutineScope(Dispatchers.Main)

		private val mLoading = MutableStateFlow(false)
		private val mPlaybackItem = MutableStateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>(com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET)

		override val playbackState: StateFlow<PlaybackState> = PlaybackState.StateFlow(player)

		override val playbackItem: StateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>
			get() = mPlaybackItem

		private val playerListener = object :
			com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener {


			override fun onMediaItemTransition(
				old: MediaItem?,
				new: MediaItem?,
				reason: com.flammky.android.medialib.temp.player.event.MediaItemTransitionReason
			) {
				mPlaybackItem.update {
					player.currentActualMediaItem
						?: com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET
				}
			}

			override fun onIsLoadingChanged(isLoading: Boolean) {
				mLoading.value = isLoading
			}
		}
		init {
			player.addListener(playerListener)
		}
	}

	interface SessionInfo {
		val playbackItem: StateFlow<com.flammky.android.medialib.common.mediaitem.MediaItem>
		val playbackState: StateFlow<PlaybackState>
	}
}
