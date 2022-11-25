package com.flammky.musicplayer.playbackcontrol.ui

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.RepeatMode
import com.flammky.musicplayer.media.playback.ShuffleMode
import com.flammky.musicplayer.playbackcontrol.ui.model.PlayPauseCommand
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackArtwork
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackDescription
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * The actual role of ViewModel here is to bridge our Composable with our Presenter,
 */
internal class PlaybackControlViewModel(
	private val coroutineDispatchers: AndroidCoroutineDispatchers,
	private val presenter: PlaybackControlPresenter
) : ViewModel() {
	private val _trackArtworkStateFlows = mutableMapOf<String, StateFlow<TrackArtwork>>()
	private val _trackDescriptionStateFlows = mutableMapOf<String, StateFlow<TrackDescription>>()

	// Consider changing these to a Function

	val repeatModeStateFlow: StateFlow<RepeatMode> =
		presenter.observeRepeatMode()
			.stateIn(viewModelScope, SharingStarted.Lazily, RepeatMode.OFF)

	val shuffleModeStateFlow: StateFlow<ShuffleMode> =
		presenter.observeShuffleMode()
			.stateIn(viewModelScope, SharingStarted.Lazily, ShuffleMode.OFF)

	val progressStateFlow: StateFlow<Duration> =
		presenter.observePlaybackProgress()
			.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackConstants.PROGRESS_UNSET)

	val bufferedProgressStateFlow: StateFlow<Duration> =
		presenter.observePlaybackBufferedProgress()
			.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackConstants.PROGRESS_UNSET)

	val durationStateFlow: StateFlow<Duration> =
		presenter.observePlaybackDuration()
			.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackConstants.DURATION_UNSET)

	val playPauseCommandStateFlow: StateFlow<PlayPauseCommand> =
		presenter.observePlayPauseCommand()
			.stateIn(viewModelScope, SharingStarted.Lazily, PlayPauseCommand.UNSET)

	@MainThread
	fun observeTrackDescription(id: String): StateFlow<TrackDescription> {
		checkInMainThread {
			"Trying to observe TrackDescription ($id) on worker Thread"
		}
		return findOrCreateTrackDescriptionStateFlow(id)
	}

	@MainThread
	fun observeTrackArtwork(id: String): StateFlow<TrackArtwork> {
		checkInMainThread {
			"Trying to observe TrackArtwork ($id) on worker Thread"
		}
		return findOrCreateTrackArtworkStateFlow(id)
	}

	@MainThread
	fun createProgressObserver() {

	}

	/**
	 * find TrackDescription Channel, create one if not found
	 */
	@MainThread
	private fun findOrCreateTrackDescriptionStateFlow(id: String): StateFlow<TrackDescription> {
		// getOrPut ?
		_trackDescriptionStateFlows[id]?.let { return it }

		val state = MutableStateFlow(TrackDescription.UNSET)

		viewModelScope.launch {
			presenter.observeTrackDescription(id).collect(state)
		}

		return state.asStateFlow().also {
			if (_trackDescriptionStateFlows.put(id, it) != null) {
				error("Duplicate TrackDescription StateFlow")
			}
		}
	}

	@MainThread
	private fun findOrCreateTrackArtworkStateFlow(id: String): StateFlow<TrackArtwork> {
		// getOrPut ?
		_trackArtworkStateFlows[id]?.let { return it }

		val state = MutableStateFlow(TrackArtwork.UNSET)

		viewModelScope.launch {
			presenter.observeTrackArtwork(id).collect(state)
		}

		return state.asStateFlow().also {
			if (_trackArtworkStateFlows.put(id, it) != null) {
				error("Duplicate TrackArtwork StateFlow")
			}
		}
	}

	private inline fun checkInMainThread(lazyMsg: () -> Any) {
		check(Looper.myLooper() == Looper.getMainLooper(), lazyMsg)
	}
}
