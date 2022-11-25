@file:OptIn(ExperimentalCoroutinesApi::class)

package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.RepeatMode
import com.flammky.musicplayer.media.playback.ShuffleMode
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.ui.model.PlayPauseCommand
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackArtwork
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackDescription
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

class RealPlaybackControlPresenter(
	private val playbackConnection: PlaybackConnection
) : PlaybackControlPresenter {

	private val repeatModeFlow = flow {
		playbackConnection.observeRepeatMode().collect(this)
	}

	private val shuffleModeFlow = flow {
		playbackConnection.observeShuffleMode().collect(this)
	}

	private val bufferedProgressFlow = flow<Duration> {

	}

	override fun observeRepeatMode(): Flow<RepeatMode> {
		return flow {
			emit(playbackConnection.getRepeatMode())
			repeatModeFlow.collect(this)
		}
	}

	override fun observeShuffleMode(): Flow<ShuffleMode> {
		return flow {
			emit(playbackConnection.getShuffleMode())
			shuffleModeFlow.collect(this)
		}
	}

	override fun observePlaybackProgress(): Flow<Duration> {
		return flow {
			emit(playbackConnection.getProgress())
			val bool = AtomicBoolean(false)
			suspend fun sendUpdate() = emit(playbackConnection.getProgress())

			coroutineScope {

				launch {
					playbackConnection.observeIsPlaying().distinctUntilChanged().collect { playing ->
						if (playing) {
							do {
								sendUpdate()
								delay(1000)
							} while (bool.get())
						} else {
							bool.set(false)
						}
					}
				}

				launch {
					playbackConnection.observeProgressDiscontinuity().collect {
						sendUpdate()
					}
				}
			}
		}
	}

	override fun observePlaybackBufferedProgress(): Flow<Duration> {
		TODO("Not yet implemented")
	}

	override fun observePlaybackDuration(): Flow<Duration> {
		TODO("Not yet implemented")
	}

	override fun observePlayPauseCommand(): Flow<PlayPauseCommand> {
		TODO("Not yet implemented")
	}

	override fun observeTrackArtwork(id: String): Flow<TrackArtwork> {
		TODO("Not yet implemented")
	}

	override fun observeTrackDescription(id: String): Flow<TrackDescription> {
		TODO("Not yet implemented")
	}

	override fun observeTrackQueue(): Flow<TrackQueue> {
		TODO("Not yet implemented")
	}

	private fun createPlaybackProgressFlow(interval: Duration): Flow<Duration> {
		return flow {

		}
	}
}
