package com.kylentt.mediaplayer.domain.musiclib.service

import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.media3.playback.PlaybackState
import com.kylentt.musicplayer.domain.musiclib.dependency.DependencyProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.interactor.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class ServiceConnector(delegate: MusicLibrary.ServiceDelegate) {
	private val dependency = DependencyProvider.Mutable()
	private val mediaController = Controller()
	private val mediaRegistry = Registry()

	val interactor = object : Interactor {

		override val controller: ControllerInteractor
			get() = mediaController.interactor

		override val info: Info
			get() = mediaRegistry.info
	}

	fun initialize(agent: Agent) {
		dependency.add(agent.dependency)
		Timber.d("ServiceConnector initialized, dependency:" +
			"\n${dependency.getDebugMessage(true)}")
	}

	fun connect(): Interactor {
		mediaController.connectMediaController()
		return interactor
	}

	private inner class Registry {
		private val _playbackStateSF = MutableStateFlow(PlaybackState.EMPTY)

		fun updatePlaybackState(state: PlaybackState) {
			_playbackStateSF.value = state
		}

		val stateInfo = object : StateInfo {
			override val playbackState: StateFlow<PlaybackState> = _playbackStateSF.asStateFlow()
		}

		val info = object : Info {
			override val state: StateInfo
				get() = stateInfo
		}
	}

	private inner class Controller {
		var state: ControllerState = ControllerState.NOTHING

		private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
		private lateinit var mediaController: MediaController

		fun connectMediaController(onConnected: (MediaController) -> Unit = {}) {
			if (state == ControllerState.CONNECTED) {
				return onConnected(mediaController)
			}
			if (state == ControllerState.CONNECTING) {
				return mediaControllerFuture
					.addListener({ onConnected(mediaController) }, MoreExecutors.directExecutor())
			}

			state = ControllerState.CONNECTING

			val context = dependency.get(Context::class.java)!!.applicationContext
			val serviceComponent = MusicLibraryService.getComponentName()
			val token = SessionToken(context, serviceComponent)
			mediaControllerFuture = MediaController.Builder(context, token)
				.setApplicationLooper(Looper.myLooper()!!)
				.setConnectionHints( /* later */ Bundle.EMPTY)
				.buildAsync()

			with(mediaControllerFuture) {
				addListener({
					checkState(isDone)
					mediaController = get()
					mediaController.addListener(playbackEventListener)
				}, MoreExecutors.directExecutor())
			}
		}

		private fun getToggledRepeat(@Player.RepeatMode mode: Int): Int {
			return when (mode) {
				Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
				Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
				Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
				else -> throw IllegalArgumentException()
			}
		}

		private val playbackEventListener = object : Player.Listener {
			override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
				val current = mediaRegistry.stateInfo.playbackState.value
				mediaRegistry.updatePlaybackState(current.copy(playWhenReady = playWhenReady))
			}

			override fun onIsPlayingChanged(isPlaying: Boolean) {
				val current = mediaRegistry.stateInfo.playbackState.value
				mediaRegistry.updatePlaybackState(current.copy(playing = isPlaying))
			}

			override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
				val current = mediaRegistry.stateInfo.playbackState.value
				val item = mediaItem ?: MediaItem.EMPTY

				if (current.mediaItem.mediaId != item.mediaId
					|| current.mediaItem.localConfiguration == null && item.localConfiguration != null
				) {
					mediaRegistry.updatePlaybackState(current.copy(mediaItem = item))
				}
			}

			override fun onPlaybackStateChanged(playbackState: Int) {
				val current = mediaRegistry.stateInfo.playbackState.value
				mediaRegistry.updatePlaybackState(current.copy(playerState = playbackState))
			}

			override fun onRepeatModeChanged(repeatMode: Int) {
				val current = mediaRegistry.stateInfo.playbackState.value
				mediaRegistry.updatePlaybackState(current.copy(playerRepeatMode = repeatMode))
			}
		}

		val interactor = object : ControllerInteractor {
			override fun play() = connectMediaController {
				if (it.playbackState.isStateIdle()) it.prepare()
				it.play()
			}
			override fun pause() = connectMediaController { it.pause() }
			override fun stop() = connectMediaController { it.stop() }
			override fun toggleRepeat() = connectMediaController { getToggledRepeat(it.repeatMode)}

			override fun setMediaItems(items: List<MediaItem>) = connectMediaController {
				val pbs = it.playbackState
				it.stop()
				it.seekTo(0, 0)
				it.setMediaItems(items)
				if (!pbs.isStateIdle()) it.prepare()
			}

			override fun seekTo(index: Int, pos: Long) = connectMediaController {
				if (it.mediaItemCount < index) return@connectMediaController
				it.seekTo(index, pos)
			}
		}
	}

	interface Info {
		val state: StateInfo
	}

	interface Interactor {
		val controller: ControllerInteractor
		val info: Info
	}

	interface ControllerInteractor {
		fun play()
		fun pause()
		fun stop()
		fun toggleRepeat()
		fun setMediaItems(items: List<MediaItem>)
		fun seekTo(index: Int, pos: Long)
	}

	interface StateInfo {
		val playbackState: StateFlow<PlaybackState>
	}

	sealed class ControllerState {
		object NOTHING : ControllerState()
		object CONNECTING : ControllerState()
		object CONNECTED : ControllerState()
		object DISCONNECTED : ControllerState()
	}
}
