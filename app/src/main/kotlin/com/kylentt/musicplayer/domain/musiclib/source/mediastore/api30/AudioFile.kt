package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api30

import android.provider.MediaStore
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio.MediaStoreAudioFile

/**
 * class representing Audio File Information on MediaStore API 30 / Android 11.0 / Android R.
 * @see MediaStore30.Files
 */
class MediaStoreAudioFile30(

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
	override val fileNameWithExtension: String,
	override val mimeType: String,
	override val size: Long,
) : MediaStoreAudioFile()
