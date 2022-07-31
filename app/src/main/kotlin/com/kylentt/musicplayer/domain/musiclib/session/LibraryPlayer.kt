package com.kylentt.musicplayer.domain.musiclib.session

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.musicplayer.domain.musiclib.dependency.Injector
import com.kylentt.musicplayer.domain.musiclib.interactor.LibraryAgent
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService
import com.kylentt.musicplayer.domain.musiclib.util.addListener
import com.kylentt.musicplayer.domain.musiclib.util.addResultListener
import com.kylentt.musicplayer.domain.musiclib.util.withEach
import java.util.concurrent.Executor
import java.util.concurrent.Future

class LibraryPlayer private constructor() {
	private val localInjector = Injector()
	private val wrapper = MediaControllerWrapper(localInjector)

	constructor(agent: LibraryAgent) : this() {
		localInjector.fuse(agent.injector)
	}

	fun connectService() = wrapper.connect()

	fun prepare() = wrapper.connect { it.prepare() }

	fun play() = wrapper.connect {
		if (it.playbackState.isStateIdle()) {
			it.prepare()
		}
		if (it.playbackState.isStateEnded()) {
			if (it.hasNextMediaItem()) it.seekToNextMediaItem()
			it.seekTo(0)
		}
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
		val state = it.playbackState

		it.stop()
		it.setMediaItems(items, startIndex, startPos)

		if (!state.isStateIdle()) it.prepare()
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

private class MediaControllerWrapper(injector: Injector) {
	private lateinit var mediaController: MediaController
	private lateinit var mediaControllerFuture: ListenableFuture<MediaController>

	private val localContext: Context? by injector.lateInject(subclass = true)
	private val localListeners: MutableList<Player.Listener> = mutableListOf()

	private val executor = Executor { it.run() }

	private var state: WrapperState = WrapperState.NOTHING

	fun connect(onConnected: (MediaController) -> Unit = {}) {
		if (state == WrapperState.CONNECTED) {
			return onConnected(mediaController)
		}
		if (state == WrapperState.CONNECTING) {
			return mediaControllerFuture.addListener(executor) { onConnected(mediaController) }
		}

		val context = localContext ?: return
		val token = SessionToken(context, MusicLibraryService.getComponentName(context))

		state = WrapperState.CONNECTING

		mediaControllerFuture = MediaController.Builder(context, token)
			.setConnectionHints( /* Later */ Bundle.EMPTY)
			.buildAsync()

		mediaControllerFuture.addResultListener(executor) { result: MediaController ->
			mediaController = result
			localListeners.forEach { listener -> result.addListener(listener) }
			onConnected(result)
			state = WrapperState.CONNECTED
		}
	}

	fun addListener(listener: Player.Listener) {
		localListeners.add(listener)
		if (state == WrapperState.CONNECTED) mediaController.addListener(listener)
	}

	fun removeListener(listener: Player.Listener) {
		localListeners.remove(listener)
		if (state == WrapperState.CONNECTED) mediaController.removeListener(listener)
	}

	fun getMediaItems(): List<MediaItem> {
		val list = mutableListOf<MediaItem>()

		if (state == WrapperState.CONNECTED) {
			val count = mediaController.mediaItemCount
			withEach(0 until count) { i -> list.add(mediaController.getMediaItemAt(i)) }
		}
		return list
	}

	private sealed class WrapperState {
		object NOTHING : WrapperState()
		object CONNECTING : WrapperState()
		object CONNECTED : WrapperState()
		object DISCONNECTED : WrapperState()
	}
}
