package com.flammky.android.medialib.temp.player

import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.LoadControl
import com.flammky.android.common.broadcast.ContextBroadcastManager
import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.internal.InternalLibraryContext
import com.flammky.android.medialib.media3.Media3Item
import com.flammky.android.medialib.temp.internal.MediaLibraryContext
import com.flammky.android.medialib.temp.player.component.AudioRerouteHandler
import com.flammky.android.medialib.temp.player.options.FallbackInfo
import com.flammky.android.medialib.temp.player.playback.PlaybackControlInfo

internal class PlayerContext private constructor(
	val looper: Looper,
	val libraryContext: MediaLibraryContext,
	val audioAttributes: AudioAttributes,
	val fallbackInfo: FallbackInfo,
	val handleAudioReroute: Pair<Boolean, AudioRerouteHandler?>,
	val playbackControlInfo: PlaybackControlInfo
) {

	val libContext = object : InternalLibraryContext(AndroidContext(libraryContext.android)) {
		override val mediaItemBuilder = InternalMediaItem.Builder { extra, mediaId, uri, metadata ->
			Media3Item.build(mediaId) {
				setExtra(extra)
				setUri(uri)
				setMetadata(metadata)
			}
		}
	}



	val android
		get() = libraryContext.android

	val broadcastManager = ContextBroadcastManager(android)

	class Builder (private val libraryContext: MediaLibraryContext) {
		private var mAudioAttributes: AudioAttributes = AudioAttributes.DEFAULT
		private var mFallbackInfo: FallbackInfo = FallbackInfo.DEFAULT
		private var mLooper: Looper = Looper.getMainLooper()
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
				looper = mLooper,
				handleAudioReroute = mHandleAudioReroute,
				playbackControlInfo = mPlaybackControlInfo
			)
		}
	}


}
