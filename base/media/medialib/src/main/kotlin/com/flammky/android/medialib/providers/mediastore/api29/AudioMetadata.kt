package com.flammky.android.medialib.providers.mediastore.api29

import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioMetadataEntryEntry

/**
 * class representing Audio File Metadata Information on MediaStore API 29 / Android 10.0 / Q.
 * @see MediaStore29.MediaColumns
 * @see MediaStore29.Audio.AudioColumns
 */
class MediaStoreAudioMetadataEntry29Entry private constructor(
	// not sure about this field yet
	@JvmField
	val dateTaken: Long,

	album: String,
	artist: String,
	bookmark: Long,
	composer: String,
	durationMs: Long,
	title: String,
	year: Int,
) : MediaStoreAudioMetadataEntryEntry(album, artist, bookmark, composer, durationMs, year, title) {

	class Builder internal constructor() {
		var album: String = ""
		var artist: String = ""
		var bookmark: Long = -1L
		var composer: String = ""
		var dateTaken: Long = -1L
		var durationMs: Long = -1L
		var title: String = ""
		var year: Int = -1

		internal fun build(): MediaStoreAudioMetadataEntry29Entry {
			return MediaStoreAudioMetadataEntry29Entry(
				album = album,
				artist = artist,
				bookmark = bookmark,
				composer = composer,
				dateTaken = dateTaken,
				durationMs = durationMs,
				title = title,
				year = year
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
