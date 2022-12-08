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

sealed interface PlaybackEvent {

	data class ProgressDiscontinuity(
		val oldProgress: Duration,
		val newProgress: Duration,
		val reason: ProgressDiscontinuityReason
	) : PlaybackEvent {
		companion object {
			val UNSET = ProgressDiscontinuity(
				oldProgress = PlaybackConstants.PROGRESS_UNSET,
				newProgress = PlaybackConstants.PROGRESS_UNSET,
				reason = ProgressDiscontinuityReason.UNKNOWN
			)
		}
	}

	data class QueueChange(
		val old: PlaybackQueue,
		val new: PlaybackQueue,
		val reason: PlaybackQueueChangeReason
	) : PlaybackEvent {
		companion object {
			val UNSET = QueueChange(
				old = PlaybackQueue.UNSET,
				new = PlaybackQueue.UNSET,
				reason = PlaybackQueueChangeReason.UNKNOWN
			)
		}
	}
}
