package com.flammky.android.medialib.providers.mediastore.api28.audio

import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioFile


/**
 * class representing Audio File Information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 * @see MediaStore28.Files
 */
class MediaStoreAudioFile28 private constructor(
	absolutePath: String?,
	dateAdded: Long?,
	dateModified: Long?,
	fileName: String?,
	mimeType: String?,
	size: Long?
) : MediaStoreAudioFile(absolutePath, dateAdded, dateModified, fileName, mimeType, size) {

	class Builder internal constructor() {
		var absolutePath: String? = null
			private set
		var dateAdded: Long? = null
			private set
		var dateModified: Long? = null
			private set
		var fileName: String? = null
			private set
		var mimeType: String? = null
			private set
		var size: Long? = null
			private set

		fun setAbsolutePath(absolutePath: String?) = apply {
			this.absolutePath = absolutePath
		}

		fun setDateAdded(dateAdded: Long?) = apply {
			this.dateAdded = dateAdded
		}

		fun setDateModified(dateModified: Long?) = apply {
			this.dateModified = dateModified
		}

		fun setFileName(fileName: String?) = apply {
			this.fileName = fileName
		}

		fun setMimeType(mimeType: String?) = apply {
			this.mimeType = mimeType
		}

		fun setSize(size: Long?) = apply {
			this.size = size
		}

		internal fun build(): MediaStoreAudioFile28 {
			return MediaStoreAudioFile28(
				absolutePath = absolutePath,
				dateAdded = dateAdded,
				dateModified = dateModified,
				fileName = fileName,
				mimeType = mimeType,
				size = size
			)
		}

	}

	companion object {
		val empty = Builder().build()
	}
}
