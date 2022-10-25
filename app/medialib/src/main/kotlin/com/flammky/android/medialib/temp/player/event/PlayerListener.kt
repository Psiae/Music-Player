package com.flammky.android.medialib.temp.player.event

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.Timeline
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Extra.Companion.toMediaItemExtra
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.common.mediaitem.PlaybackMetadata
import com.flammky.android.medialib.temp.player.LibraryPlayer
import com.flammky.android.medialib.temp.player.LibraryPlayer.PlaybackState.Companion.asPlaybackState
import com.flammky.android.medialib.temp.player.PlayerContext
import com.flammky.android.medialib.temp.player.PlayerContextInfo
import com.flammky.android.medialib.temp.player.event.MediaItemTransitionReason.Companion.asMediaItemTransitionReason
import com.flammky.android.medialib.temp.player.event.PlayWhenReadyChangedReason.Companion.asPlayWhenReadyChangedReason
import com.flammky.android.medialib.temp.player.playback.RepeatMode
import com.flammky.android.medialib.temp.player.playback.RepeatMode.Companion.asRepeatMode
import com.flammky.android.medialib.temp.player.playback.RepeatMode.Companion.toRepeatModeInt
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

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

	fun onLibMediaItemTransition(oldLib: com.flammky.android.medialib.common.mediaitem.MediaItem?, newLib: com.flammky.android.medialib.common.mediaitem.MediaItem?) {}

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
	fun onTimelineChanged(old: com.flammky.android.medialib.temp.media3.Timeline, new: com.flammky.android.medialib.temp.media3.Timeline, reason: Int) {}

	fun onPositionDiscontinuity(
		oldPos: PositionInfo,
		newPos: PositionInfo,
		@Player.DiscontinuityReason reason: Int
	) {}

	fun libOnPositionDiscontinuity(oldPosition: Long, newPosition: Long, reason: Int) {}

	fun onShuffleModeEnabledChanged(enabled: Boolean) {}

	companion object {
		@Deprecated("Temporary")
		internal fun LibraryPlayerEventListener.asPlayerListener(player: LibraryPlayer, playerContext: PlayerContext): Player.Listener {
			return object : Player.Listener {
				val delegated = this@asPlayerListener

				private var rememberIsPlaying = player.isPlaying
				private var rememberTimeline = player.timeLine
				private var rememberRepeatMode: Int = player.repeatMode.toRepeatModeInt

				private var rememberLibTimeline = com.flammky.android.medialib.temp.media3.Timeline(
					Contract.DURATION_UNSET
				)

				override fun onEvents(player: Player, events: Player.Events) {

				}

				override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
					delegated.onShuffleModeEnabledChanged(shuffleModeEnabled)
				}

				override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
					delegated.onAudioAttributesChanged(audioAttributes, audioAttributes)
				}

				override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
					delegated.onAvailableCommandsChanged(availableCommands)
				}

				private var rememberOldItem: MediaItem? = null
				private var rememberOldLibItem: com.flammky.android.medialib.common.mediaitem.MediaItem? = null
				override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
					Timber.d("onMediaItemTransition $mediaItem, ${mediaItem?.mediaId}, ${mediaItem?.localConfiguration}, $reason, index: ${player.currentMediaItemIndex}")
					if (mediaItem?.localConfiguration == null) {
						delegated.onMediaItemTransition(rememberOldItem, mediaItem, reason.asMediaItemTransitionReason)
						rememberOldItem = mediaItem
						val libItem = mediaItem?.let { convertMediaItem(mediaItem) }
						delegated.onLibMediaItemTransition(
							rememberOldLibItem,
							libItem
						)
						rememberOldLibItem = libItem
					}
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
						val libTimeline = com.flammky.android.medialib.temp.media3.Timeline(player.durationMs.milliseconds)
						delegated.onTimelineChanged(rememberLibTimeline, libTimeline, reason)
						rememberLibTimeline = libTimeline
					}
				}

				override fun onPositionDiscontinuity(
					oldPosition: PositionInfo,
					newPosition: PositionInfo,
					reason: Int
				) {
					delegated.onPositionDiscontinuity(oldPosition, newPosition, reason)
					delegatePositionDiscontinuity(oldPosition.positionMs, newPosition.positionMs, reason)
				}

				private fun delegatePositionDiscontinuity(oldPos: Long, newPos: Long, reason: Int) {
					delegated.libOnPositionDiscontinuity(oldPos, newPos, reason)
				}

				@Deprecated("Temporary", ReplaceWith("TODO"))
				private fun convertMediaItem(item: androidx.media3.common.MediaItem): com.flammky.android.medialib.common.mediaitem.MediaItem {
					return playerContext.libContext.buildMediaItem {
						setMediaUri(item.localConfiguration?.uri ?: item.requestMetadata.mediaUri ?: Uri.EMPTY)
						setMediaId(item.mediaId)
						setExtra(item.requestMetadata.extras?.toMediaItemExtra() ?: com.flammky.android.medialib.common.mediaitem.MediaItem.Extra.UNSET)

						val hint = item.requestMetadata.extras!!.getString("mediaMetadataType")!!
						val mediaMetadata = item.mediaMetadata

						val metadata = when {
							hint.startsWith("audio;") -> {
								AudioMetadata.build {
									mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
									setAlbumArtist(mediaMetadata.albumArtist?.toString())
									setAlbumTitle(mediaMetadata.albumTitle?.toString())
									setArtist(mediaMetadata.artist?.toString())
									setBitrate(mediaMetadata.extras?.getString("bitrate")?.toLong())
									setDuration(mediaMetadata.extras?.getString("durationMs")?.toLong()?.milliseconds)
									setPlayable(mediaMetadata.isPlayable)
									setTitle(mediaMetadata.title?.toString())
								}
							}
							hint.startsWith("playback;") -> {
								PlaybackMetadata.build {
									mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
									setBitrate(mediaMetadata.extras?.getString("bitrate")?.toLong())
									setDuration(mediaMetadata.extras?.getString("durationMs")?.toLong()?.milliseconds)
									setPlayable(mediaMetadata.isPlayable)
									setTitle(mediaMetadata.title?.toString())
								}
							}
							hint.startsWith("base;") -> {
								MediaMetadata.build {
									mediaMetadata.extras?.let { setExtra(MediaMetadata.Extra(it)) }
									setTitle(mediaMetadata.title?.toString())
								}
							}
							else -> error("Invalid Media3Item Internal Info")
						}

						setMetadata(metadata)
					}
				}
			}
		}
	}
}


