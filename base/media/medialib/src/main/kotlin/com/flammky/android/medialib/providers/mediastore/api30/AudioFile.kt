package com.flammky.android.medialib.providers.mediastore.api30

import android.provider.MediaStore
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioFile

/**
 * class representing Audio File Information on MediaStore API 30 / Android 11.0 / Android R.
 * @see MediaStore30.Files
 */
class MediaStoreAudioFile30 private constructor(

	/**
	 * the bucket (folder) name of the file,
	 *
	 * @see [MediaStore.MediaColumns.BUCKET_DISPLAY_NAME]
	 */
	val bucketDisplayName: String,

	/**
	 * the bucket (folder) id of the file,
	 * + computed from [absolutePath] if this column is not present
	 *
	 * ```
	 * val parentFile = File(absolutePath).parentFile ?: File("/")
	 * parentFile.toString().lowerCase().hashCode()
	 * ```
	 *
	 * @see [MediaStore.MediaColumns.BUCKET_ID]
	 */
	val bucketId: Long,

	absolutePath: String,
	dateAdded: Long,
	dateModified: Long,
	fileName: String,
	mimeType: String,
	size: Long,
) : MediaStoreAudioFile(absolutePath, dateAdded, dateModified, fileName, mimeType, size) {

	class Builder internal constructor() {
		var absolutePath = ""
		var bucketDisplayName = ""
		var bucketId: Long = Long.MIN_VALUE
		var dateAdded: Long = -1
		var dateModified: Long = -1
		var fileName: String = ""
		var mimeType: String = ""
		var size: Long = -1L

		internal fun build(): MediaStoreAudioFile30 {
			return MediaStoreAudioFile30(
				absolutePath = absolutePath,
				bucketDisplayName = bucketDisplayName,
				bucketId = bucketId,
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
