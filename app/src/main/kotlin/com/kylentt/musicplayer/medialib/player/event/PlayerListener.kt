package com.kylentt.musicplayer.medialib.player.event

import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.Command
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.Timeline
import com.kylentt.musicplayer.medialib.player.PlayerContextInfo
import com.kylentt.musicplayer.medialib.player.LibraryPlayer
import com.kylentt.musicplayer.medialib.player.LibraryPlayer.PlaybackState.Companion.asPlaybackState
import com.kylentt.musicplayer.medialib.player.event.MediaItemTransitionReason.Companion.asMediaItemTransitionReason
import com.kylentt.musicplayer.medialib.player.event.PlayWhenReadyChangedReason.Companion.asPlayWhenReadyChangedReason
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode.Companion.asRepeatMode
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode.Companion.toRepeatModeInt

interface LibraryPlayerEventListener {

	/**
	 * Called when current [PlayerContextInfo.audioAttributes] of [LibraryPlayer] changes
	 *
	 * @param old the value of [PlayerContextInfo.audioAttributes] after it changes
	 * @param new the value of [PlayerContextInfo.audioAttributes] before it changes
	 */
	fun onAudioAttributesChanged(old: AudioAttributes, new: AudioAttributes) {}

	/**
	 * Called when current [LibraryPlayer.availableCommands] changes
	 *
	 * @param availableCommand the new commands
	 */
	fun onAvailableCommandsChanged(availableCommand: Player.Commands) {}

	/**
	 * Called when current [LibraryPlayer.currentMediaItem] changes
	 *
	 * Note:
	 * + This callback is also called when the playlist becomes non-empty or empty as a
	 * 	 consequence of a playlist change.
	 *
	 * + This callback is also called when [MediaItem] is repeated because of [RepeatMode]
	 *
	 * @see [MediaItemTransitionReason] for more detailed explanation
	 *
	 * @param [old] the old value of [LibraryPlayer.currentMediaItem] before transition, may be null
	 * @param [new] the new value of [LibraryPlayer.currentMediaItem] after transition, may be null
	 * @param [reason] the reason
	 */
	fun onMediaItemTransition(old: MediaItem?, new: MediaItem?, reason: MediaItemTransitionReason) {}

	/**
	 * Called when current [LibraryPlayer.repeatMode] changes
	 *
	 * @param [old] the old value of [LibraryPlayer.repeatMode] before it changes
	 * @param [new] the new value of [LibraryPlayer.repeatMode] after it changes
	 */
	fun onRepeatModeChanged(old: RepeatMode, new: RepeatMode) {}

	/**
	 * Called when current [LibraryPlayer.playWhenReady] changes
	 *
	 * @param [playWhenReady] the new value of [LibraryPlayer.playWhenReady]
	 * @param [reason] the reason
	 */
	fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: PlayWhenReadyChangedReason) {}

	/**
	 * Called when current [LibraryPlayer.playbackState] changes
	 *
	 * @param [state] the new value of [LibraryPlayer.playbackState]
	 */
	fun onPlaybackStateChanged(state: LibraryPlayer.PlaybackState) {}

	/**
	 * Called when current [LibraryPlayer.isPlaying] changes
	 *
	 * @param [isPlaying] the new value of [LibraryPlayer.playWhenReady]
	 * @param [reason] the reason
	 */
	fun onIsPlayingChanged(isPlaying: Boolean, reason: IsPlayingChangedReason) {}

	/**
	 * Called when current [LibraryPlayer.isLoading] changes
	 *
	 * @param [isLoading] the new value of [LibraryPlayer.isLoading]
	 */
	fun onIsLoadingChanged(isLoading: Boolean, /* IsLoadingChangedReason */) {}

	/**
	 * Called when current [LibraryPlayer.timeLine] changes
	 *
	 * @param old the value of [LibraryPlayer.timeLine] after it changes
	 * @param new the value of [LibraryPlayer.timeLine] before it changes
	 */
	fun onTimelineChanged(old: Timeline, new: Timeline, @Player.TimelineChangeReason reason: Int) {}

	fun onPositionDiscontinuity(
		oldPos: PositionInfo,
		newPos: PositionInfo,
		@Player.DiscontinuityReason reason: Int
	) {}

	companion object {
		@Deprecated("Temporary")
		fun LibraryPlayerEventListener.asPlayerListener(player: LibraryPlayer): Player.Listener {
			return object : Player.Listener {
				val delegated = this@asPlayerListener

				private var rememberIsPlaying = player.isPlaying
				private var rememberTimeline = player.timeLine
				private var rememberRepeatMode: Int = player.repeatMode.toRepeatModeInt

				override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
					delegated.onAudioAttributesChanged(audioAttributes, audioAttributes)
				}

				override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
					delegated.onAvailableCommandsChanged(availableCommands)
				}

				private var rememberOldItem: MediaItem? = null
				override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
					delegated.onMediaItemTransition(rememberOldItem, mediaItem, reason.asMediaItemTransitionReason)
					rememberOldItem = mediaItem
				}


				override fun onRepeatModeChanged(repeatMode: Int) {
					if (repeatMode != rememberRepeatMode) {
						delegated.onRepeatModeChanged(rememberRepeatMode.asRepeatMode, repeatMode.asRepeatMode)
						rememberRepeatMode = repeatMode
					}
				}

				override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
					delegated.onPlayWhenReadyChanged(playWhenReady, reason.asPlayWhenReadyChangedReason)

					if (!playWhenReady && rememberIsPlaying) {
						delegated.onIsPlayingChanged(false, IsPlayingChangedReason.PLAY_WHEN_READY_CHANGED)
						rememberIsPlaying = false
					}
				}

				override fun onPlaybackStateChanged(playbackState: Int) {
					val cast = playbackState.asPlaybackState
					delegated.onPlaybackStateChanged(cast)

					when {
						cast !== LibraryPlayer.PlaybackState.READY && rememberIsPlaying -> {
							delegated.onIsPlayingChanged(false, IsPlayingChangedReason.PLAYBACK_STATE_CHANGED)
							rememberIsPlaying = false
						}
					}
				}


				override fun onIsPlayingChanged(isPlaying: Boolean) {
					if (isPlaying != rememberIsPlaying) {
						delegated.onIsPlayingChanged(isPlaying, IsPlayingChangedReason.UNKNOWN)
						rememberIsPlaying = isPlaying
					}
				}

				override fun onIsLoadingChanged(isLoading: Boolean) {
					delegated.onIsLoadingChanged(isLoading)
				}


				override fun onTimelineChanged(timeline: Timeline, reason: Int) {
					if (timeline != rememberTimeline) {
						delegated.onTimelineChanged(rememberTimeline, timeline, reason)
						rememberTimeline = timeline
					}
				}

				override fun onPositionDiscontinuity(
					oldPosition: PositionInfo,
					newPosition: PositionInfo,
					reason: Int
				) {
					delegated.onPositionDiscontinuity(oldPosition, newPosition, reason)
				}
			}
		}
	}
}


