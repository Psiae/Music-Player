package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import com.flammky.android.medialib.temp.provider.mediastore.api28.MediaStore28
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioMetadata

/**
 * class representing Audio File Metadata Information on MediaStore API 29 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 * @see MediaStore28.Audio
 */

class MediaStoreAudioMetadata28 private constructor(
	override val album: String,
	override val artist: String,
	override val bookmark: Long,
	override val composer: String,
	override val durationMs: Long,
	override val title: String,
	override val year: Int
) : MediaStoreAudioMetadata() {

	class Builder internal constructor() {
		var album: String = ""
		var artist: String = ""
		var bookmark: Long = -1
		var composer: String = ""
		var durationMs: Long = -1
		var title: String = ""
		var year: Int = -1

		internal fun build(): MediaStoreAudioMetadata28 {
			return MediaStoreAudioMetadata28(
				album = album,
				artist = artist,
				bookmark = bookmark,
				composer = composer,
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
