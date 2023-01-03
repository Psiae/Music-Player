package com.flammky.musicplayer.playbackcontrol.ui

import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.playbackcontrol.ui.model.PlayPauseCommand
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackArtwork
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackDescription
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackQueue
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * The Responsibility of the Presenter here is to map the incoming data as such they can be
 * efficiently displayed, have abstract knowledge of the View, has no knowledge of View lifecycle
 */
interface PlaybackControlPresenter {

	fun observeRepeatMode(): Flow<RepeatMode>

	fun observeShuffleMode(): Flow<ShuffleMode>

	fun observePlaybackProgress(): Flow<Duration>

	fun observePlaybackBufferedProgress(): Flow<Duration>

	fun observePlaybackDuration(): Flow<Duration>

	fun observePlayPauseCommand(): Flow<PlayPauseCommand>

	fun observeTrackArtwork(id: String): Flow<TrackArtwork>

	fun observeTrackDescription(id: String): Flow<TrackDescription>

	fun observeTrackQueue(): Flow<TrackQueue>
}
