package com.flammky.musicplayer.media.playback

import kotlin.time.Duration

sealed interface ProgressDiscontinuityReason {

	object UNKNOWN : ProgressDiscontinuityReason

	object USER_SEEK : ProgressDiscontinuityReason
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
		val old: RepeatMode,
		val new: RepeatMode,
		val reason: RepeatModeChangeReason
	) : PlaybackEvent

	data class ShuffleModeChange(
		val old: ShuffleMode,
		val new: ShuffleMode,
		val reason: ShuffleModeChangeReason
	) : PlaybackEvent

	data class ProgressDiscontinuity(
		val oldProgress: Duration,
		val newProgress: Duration,
		val reason: ProgressDiscontinuityReason
	) : PlaybackEvent

	data class QueueChange(
		val old: PlaybackQueue,
		val new: PlaybackQueue,
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
