package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio.MediaStoreAudioFile

/**
 * class representing Audio File Information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.Files
 */
class MediaStoreAudioFile28 private constructor(
	override val absolutePath: String,
	override val dateAdded: Long,
	override val dateModified: Long,
	override val fileName: String,
	override val fileNameWithExtension: String,
	override val mimeType: String,
	override val size: Long
) : MediaStoreAudioFile() {

	class Builder internal constructor() {
		var absolutePath: String = ""
		var dateAdded: Long = -1L
		var dateModified: Long = -1L
		var fileName: String = ""
		var fileNameWithExtension: String = ""
		var mimeType: String = ""
		var size: Long = -1L

		internal fun build(): MediaStoreAudioFile28 {
			return MediaStoreAudioFile28(
				absolutePath = absolutePath,
				dateAdded = dateAdded,
				dateModified = dateModified,
				fileName = fileName,
				fileNameWithExtension = fileNameWithExtension,
				mimeType = mimeType,
				size = size
			)
		}

	}

	companion object {
		val empty = Builder().build()
	}
}
