package com.kylentt.musicplayer.domain.musiclib.session

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent
import com.kylentt.musicplayer.domain.musiclib.player.MediaControllerWrapper

class LibraryPlayer(private val agent: LibraryAgent) {
	private val wrapper = MediaControllerWrapper(agent.context)

	val position: Long
		get() = wrapper.position

	val duration: Long
		get() = wrapper.duration

	init {
		wrapper.connect()
	}

	fun preparePlayback() = wrapper.connect { it.prepareResource() }

	fun play() = wrapper.connect {
		if (it.playerState.isStateIdle()) {
			it.prepareResource()
		}
		if (it.playerState.isStateEnded()) {
			if (it.hasNextMediaItem) it.seekToNextMediaItem()
			it.seekToPosition(0)
		}
		it.play()
	}

	fun play(item: MediaItem) = wrapper.connect {
		if (it.currentMediaItem?.mediaId == item.mediaId) {
			play()
			return@connect
		}

		if (it.playerState.isOngoing()) it.stop()

		it.setMediaItem(item, 0)
		it.prepareResource()

		it.play()
	}

	fun pause() = wrapper.connect {
		it.pause()
	}

	fun setMediaItems(
		items: List<MediaItem>,
		startIndex: Int = 0,
		startPos: Long = 0
	) = wrapper.connect {
		val state = it.playerState

		it.stop()
		it.setMediaItems(items, startIndex, startPos)

		if (!state.isStateIdle()) it.prepareResource()
	}

	fun addListener(listener: Player.Listener) {
		wrapper.addListener(listener)
	}

	fun removeListener(listener: Player.Listener) {
		wrapper.removeListener(listener)
	}

	// maybe cache it
	fun getMediaItems(): List<MediaItem> = wrapper.getMediaItems()
}
