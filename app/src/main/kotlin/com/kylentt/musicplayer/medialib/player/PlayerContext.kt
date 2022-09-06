package com.kylentt.musicplayer.medialib.player

import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.LoadControl
import com.kylentt.musicplayer.common.android.broadcast.ContextBroadcastManager
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.player.component.AudioRerouteHandler
import com.kylentt.musicplayer.medialib.player.options.FallbackInfo
import com.kylentt.musicplayer.medialib.player.playback.PlaybackControlInfo

class PlayerContext private constructor(
	private val libraryContext: MediaLibraryContext,
	val audioAttributes: AudioAttributes,
	val fallbackInfo: FallbackInfo,
	val handleAudioReroute: Pair<Boolean, AudioRerouteHandler?>,
	val playbackControlInfo: PlaybackControlInfo
) {

	val android
		get() = libraryContext.android

	val broadcastManager = ContextBroadcastManager(android)

	class Builder (private val libraryContext: MediaLibraryContext) {
		private var mAudioAttributes: AudioAttributes = AudioAttributes.DEFAULT
		private var mFallbackInfo: FallbackInfo = FallbackInfo.DEFAULT
		private var mHandleAudioReroute: Pair<Boolean, AudioRerouteHandler?> = true to null
		private var mPlaybackControlInfo: PlaybackControlInfo = PlaybackControlInfo.DEFAULT

		// TODO
		private var mLoadControl: LoadControl? = null


		fun setAudioAttributes(audioAttributes: AudioAttributes): Builder {
			mAudioAttributes = audioAttributes
			return this
		}

		/**
		 * @param handle whether we should handle AudioReroute
		 * @param handler optional self-handling option, handled internally if null
		 */
		fun setHandleAudioReroute(handle: Boolean, handler: AudioRerouteHandler?): Builder {
			mHandleAudioReroute = handle to handler
			return this
		}

		fun setLoadControl(loadControl: LoadControl): Builder {
			mLoadControl = loadControl
			return this
		}

		fun setPlaybackControlInfo(playbackControlInfo: PlaybackControlInfo): Builder {
			mPlaybackControlInfo = playbackControlInfo
			return this
		}

		fun build(): PlayerContext {
			return PlayerContext(
				libraryContext = libraryContext,
				audioAttributes = mAudioAttributes,
				fallbackInfo = mFallbackInfo,
				handleAudioReroute = mHandleAudioReroute,
				playbackControlInfo = mPlaybackControlInfo
			)
		}
	}


}
