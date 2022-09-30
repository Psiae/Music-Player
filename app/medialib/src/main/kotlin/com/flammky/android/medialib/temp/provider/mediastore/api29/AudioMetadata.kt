package com.flammky.android.medialib.temp.provider.mediastore.api29

import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioMetadata

/**
 * class representing Audio File Metadata Information on MediaStore API 29 / Android 10.0 / Q.
 * @see MediaStore29.MediaColumns
 * @see MediaStore29.Audio.AudioColumns
 */
class MediaStoreAudioMetadata29 private constructor(
	override val album: String,
	override val artist: String,
	override val bookmark: Long,
	override val composer: String,
	override val durationMs: Long,
	override val title: String,
	override val year: Int,

	// not sure about this field yet
	val dateTaken: Long,
) : MediaStoreAudioMetadata() {

	class Builder internal constructor() {
		var album: String = ""
		var artist: String = ""
		var bookmark: Long = -1L
		var composer: String = ""
		var dateTaken: Long = -1L
		var durationMs: Long = -1L
		var title: String = ""
		var year: Int = -1

		internal fun build(): MediaStoreAudioMetadata29 {
			return MediaStoreAudioMetadata29(
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
