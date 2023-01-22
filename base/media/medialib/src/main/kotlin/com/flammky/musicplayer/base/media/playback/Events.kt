package com.flammky.musicplayer.base.media.playback

import kotlin.time.Duration

sealed interface PositionDiscontinuityReason {

	object UNKNOWN : PositionDiscontinuityReason

	object USER_SEEK : PositionDiscontinuityReason
}

sealed interface PlaybackQueueChangeReason {

	object UNKNOWN : PlaybackQueueChangeReason

	object AUTO_TRANSITION : PlaybackQueueChangeReason

	object MODIFY_QUEUE : PlaybackQueueChangeReason
}

sealed interface RepeatModeChangeReason {
	object UNKNOWN : RepeatModeChangeReason
}

sealed interface ShuffleModeChangeReason {
	object UNKNOWN : ShuffleModeChangeReason
}

sealed interface PlaybackSpeedChangeReason {
	object UNKNOWN : PlaybackSpeedChangeReason
}

sealed interface IsPlayingChangeReason {
	object UNKNOWN : IsPlayingChangeReason
}

sealed interface PlaybackEvent {

	data class RepeatModeChange(
		val old: RepeatMode?,
		val new: RepeatMode,
		val reason: RepeatModeChangeReason
	) : PlaybackEvent

	data class ShuffleModeChange(
		val old: ShuffleMode?,
		val new: ShuffleMode,
		val reason: ShuffleModeChangeReason
	) : PlaybackEvent

	data class PositionDiscontinuity(
		val oldPosition: Duration?,
		val newPosition: Duration,
		val reason: PositionDiscontinuityReason
	) : PlaybackEvent

	data class QueueChange(
        val old: OldPlaybackQueue,
        val new: OldPlaybackQueue,
        val reason: PlaybackQueueChangeReason
	) : PlaybackEvent

	data class PlaybackSpeedChange(
		val old: Float,
		val new: Float,
		val reason: PlaybackQueueChangeReason
	) : PlaybackEvent

	data class IsPlayingChange(
		val old: Boolean,
		val new: Boolean,
		val reason: IsPlayingChangeReason
	) : PlaybackEvent
}
