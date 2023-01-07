package com.flammky.musicplayer.base.media.playback

import com.flammky.musicplayer.base.BuildConfig
import com.flammky.musicplayer.base.media.playback.PlaybackProperties.PlaybackState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Data class holding Playback Properties info, this is just a bulk list for convenience
 * will separate them if necessary
 */
data class PlaybackProperties(

	/**
	 * The state of the Playback.
	 * @see PlaybackState
	 */
	val playbackState: PlaybackState,

	/**
	 * whether the playback should resume when [playbackState] is [PlaybackState.READY].
	 */
	val playWhenReady: Boolean,

	/**
	 * whether the playback will resume when [playbackState] is [PlaybackState.READY].
	 */
	val canPlayWhenReady: Boolean,

	/**
	 * whether the play command is available
	 */
	val canPlay: Boolean,

	/**
	 * whether the playback is actually playing.
	 * playback configuration change might not have immediate effect
	 */
	val playing: Boolean,

	/**
	 * whether the playback loader is currently buffering for resources.
	 * does not necessarily mean that it is required to resume playback, this includes `Pre-Buffering`
	 */
	val loading: Boolean,

	/**
	 * speed configuration of the playback when playing
	 */
	val speed: Float,

	/**
	 * whether the playback has next media item, note that [repeatMode] will have effect on this.
	 * [RepeatMode.ALL] will always make this true regardless of actual queue
	 */
	val hasNextMediaItem: Boolean,

	/**
	 * whether the `seekNext` command is available
	 */
	val canSeekNext: Boolean,

	/**
	 * whether the playback has next media item, note that [repeatMode] will have effect on this.
	 * [RepeatMode.ALL] will always make this true regardless of actual queue
	 */
	val hasPreviousMediaItem: Boolean,

	/**
	 * whether the `seekPrevious` command is available
	 */
	val canSeekPrevious: Boolean,

	/**
	 * current [ShuffleMode]
	 */
	val shuffleMode: ShuffleMode,

	val canShuffleOn: Boolean,

	val canShuffleOff: Boolean,

	/**
	 * current [RepeatMode]
	 */
	val repeatMode: RepeatMode,

	val canRepeatOne: Boolean,

	val canRepeatAll: Boolean,

	val canRepeatOff: Boolean,

	/**
	 * List of playback suppression, reason playback are not advancing.
	 *
	 * still thinking on what count as `suppression`, other than audio-focus loss, does error count ?
	 */
	// TODO: Sealed Class
	val playbackSuppression: ImmutableList<Any>
) {

	init {
		// UI-wise there should not be any problem
		if (BuildConfig.DEBUG) {
			assert()
		}
	}

	// Data assertion, just show how `safe` the data are given the current capability of the provider
	@Suppress("KotlinConstantConditions", "ControlFlowWithEmptyBody")
	private fun assert() {
		// TODO: Assertion
		when (playbackState) {
			PlaybackState.IDLE -> {
				// does PlaybackState.IDLE == !playing ?
			}
			PlaybackState.BUFFERING -> {
				// does PlaybackState.BUFFERING == !playing ?
				/*require(loading) {
					"Inconsistent info: playbackState=$playbackState was accompanied by wrong " +
						"loading=$loading"
				}*/
			}
			PlaybackState.READY -> {
				Unit
			}
			PlaybackState.ENDED -> {
				// does PlaybackState.ENDED == !playing ?
				/*require(!hasNextMediaItem) {
					"Inconsistent info: playbackState=$playbackState was accompanied by wrong " +
						"hasNextMediaItem=$hasNextMediaItem"
				}
				require(!canSeekNext) {
					"Inconsistent info: playbackState=$playbackState was accompanied by wrong " +
						"canSeekNext=$canSeekNext"
				}*/
			}
		}
		if (canPlayWhenReady) {
			require(!playWhenReady) {
				"Inconsistent info: canPlayWhenReady=$canPlayWhenReady was accompanied by wrong " +
					"playWhenReady=$playWhenReady"
			}
		}
		if (canSeekNext) {
			if (!hasNextMediaItem) {
				if (repeatMode != RepeatMode.OFF) {
					require(playbackState != PlaybackState.ENDED) {
						"Inconsistent info: canSeekNext=$canSeekNext was accompanied by wrong " +
							"playbackState=$playbackState"
					}
				} else {
					throw IllegalArgumentException(
						"Inconsistent info: canSeekNext=$canSeekNext was accompanied by wrong " +
							"repeatMode=$repeatMode"
					)
				}
			}
		}
		if (playing) {
			// does `PlaybackState.Ready && playWhenReady` == playing ?
		}
		if (loading) {
			// I Don't think there's anything to assert
		}
		require(speed > 0f) {
			"Invalid info: speed=$speed must be more than 0f"
		}
	}

	/**
	 * Playback state
	 */
	sealed interface PlaybackState {

		/**
		 * indicates that the player is not ready to buffer for resources and need to prepare
		 */
		object IDLE : PlaybackState

		/**
		 * indicates that the player need to buffer for resources in order to resume playback
		 *
		 * **
		 * Pre-Buffering does Not count as `BUFFERING`
		 * @see [loading] instead
		 * **
		 */
		object BUFFERING : PlaybackState

		/**
		 * indicates that the player has buffered resources that is ready to resume playback
		 */
		object READY : PlaybackState

		/**
		 * indicates that the player has reached the end of the possible seek position within its queue
		 */
		object ENDED : PlaybackState
	}

	companion object {
		val UNSET = PlaybackProperties(
			playbackState = PlaybackState.IDLE,
			playWhenReady = false,
			canPlayWhenReady = false,
			canPlay = false,
			playing = false,
			loading = false,
			speed = 1f,
			hasNextMediaItem = false,
			canSeekNext = false,
			hasPreviousMediaItem = false,
			canSeekPrevious = false,
			shuffleMode = ShuffleMode.OFF,
			canShuffleOn = false,
			canShuffleOff = false,
			repeatMode = RepeatMode.OFF,
			canRepeatOne = false,
			canRepeatAll = false,
			canRepeatOff = false,
			playbackSuppression = persistentListOf()
		)
	}
}
