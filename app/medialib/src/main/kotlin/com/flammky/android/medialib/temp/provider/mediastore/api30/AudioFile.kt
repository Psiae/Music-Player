package com.flammky.android.medialib.temp.provider.mediastore.api30

import android.provider.MediaStore
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioFile

/**
 * class representing Audio File Information on MediaStore API 30 / Android 11.0 / Android R.
 * @see MediaStore30.Files
 */
class MediaStoreAudioFile30 private constructor(

	override val absolutePath: String,

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

	override val dateAdded: Long,
	override val dateModified: Long,
	override val fileName: String,
	override val mimeType: String,
	override val size: Long,
) : MediaStoreAudioFile() {

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
