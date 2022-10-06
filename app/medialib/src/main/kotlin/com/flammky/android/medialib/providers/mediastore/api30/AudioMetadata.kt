package com.flammky.android.medialib.providers.mediastore.api30

import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioMetadata

class MediaStoreAudioMetadata30 private constructor(
	// not sure about this field yet
	val dateTaken: Long,
	val albumArtist: String,
	val bitRate: Long,

	album: String,
	artist: String,
	bookmark: Long,
	composer: String,
	durationMs: Long,
	title: String,
	year: Int,
) : MediaStoreAudioMetadata(album, artist, bookmark, composer, durationMs, year, title) {

	class Builder internal constructor() {
		var album: String = ""
		var artist: String = ""
		var bookmark: Long = -1L
		var composer: String = ""
		var durationMs: Long = -1L
		var title: String = ""
		var year: Int = -1

		var dateTaken: Long = -1L
		var albumArtist: String = ""
		var bitRate: Long = -1

		internal fun build(): MediaStoreAudioMetadata30 {
			return MediaStoreAudioMetadata30(
				album = album,
				artist = artist,
				bookmark = bookmark,
				composer = composer,
				durationMs = durationMs,
				title = title,
				year = year,
				dateTaken = dateTaken,
				albumArtist = albumArtist,
				bitRate = bitRate
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
