package com.flammky.musicplayer.android.main

import android.content.ContentResolver
import com.flammky.musicplayer.android.intent.AndroidIntent
import com.flammky.musicplayer.android.intent.scheme
import kotlinx.collections.immutable.persistentSetOf

class LocalPlaybackRequestIntentCapability {

	fun checkDescription(
		intent: AndroidIntent
	): Boolean {

		intent.scheme
			?.let { scheme ->
				if (scheme !in LOCAL_PLAYBACK_REQUEST_SUPPORTED_URI_SCHEME) {
					return false
				}
			}
			?: return false

		intent.mimeType
			?.let { mime ->
				if (mime != "audio/*" && mime !in LOCAL_PLAYBACK_REQUEST_SUPPORTED_MIME) {
					return false
				}
			}
			?: return false

		return true
	}

	companion object {
		private val LOCAL_PLAYBACK_REQUEST_SUPPORTED_URI_SCHEME = persistentSetOf(
			ContentResolver.SCHEME_CONTENT,
			/*ContentResolver.SCHEME_FILE*/
		)

		// https://developer.android.com/guide/topics/media/exoplayer/supported-formats
		private val LOCAL_PLAYBACK_REQUEST_SUPPORTED_MIME = persistentSetOf(
			"audio/aac",
			"audio/aiff",
			"audio/basic",
			"audio/flac",
			"audio/midi",
			"audio/mp4",
			"audio/mpeg",
			"audio/ogg",
			"audio/opus",
			"audio/vorbis",
			"audio/wav",
			"audio/webm",
			"audio/x-aiff",
			"audio/x-matroska",
			"audio/x-wav",
		)
	}
}
