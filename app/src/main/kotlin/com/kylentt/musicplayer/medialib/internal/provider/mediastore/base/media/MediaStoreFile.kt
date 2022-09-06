package com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.media

import android.provider.MediaStore
import com.kylentt.musicplayer.common.kotlin.time.annotation.DurationValue
import com.kylentt.musicplayer.domain.musiclib.annotation.StorageDataUnit
import com.kylentt.musicplayer.domain.musiclib.annotation.StorageDataValue
import java.util.concurrent.TimeUnit

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#471)
 */

abstract class MediaStoreFile internal constructor() {

	/**
	 * the absolute path of the file
	 *
	 * @see [MediaStore.Files.FileColumns.DATA]
	 */
	abstract val absolutePath: String

	/**
	 * the `date-added` of this file,
	 * the value represent elapsed [TimeUnit.SECONDS] since Unix Epoch Time.
	 *
	 * @see [MediaStore.Files.FileColumns.DATE_ADDED]
	 */
	@DurationValue(TimeUnit.SECONDS)
	abstract val dateAdded: Long

	/**
	 * the `date-modified` of this file,
	 * the value represent elapsed [TimeUnit.SECONDS] since Unix Epoch Time.
	 *
	 * @see [MediaStore.Files.FileColumns.DATE_MODIFIED]
	 */
	@DurationValue(TimeUnit.SECONDS)
	abstract val dateModified: Long

	/**
	 * the name of the file, including its extension
	 *
	 * @see [MediaStore.Files.FileColumns.DISPLAY_NAME]
	 */
	abstract val fileName: String

	/**
	 * The media type (audio, video, image or playlist) of the file,
	 * or 0 for not a media file
	 *
	 * @see [MediaStore.Files.FileColumns.MEDIA_TYPE]
	 */
	abstract val mediaType: Int

	/**
	 * the Mime-Type of the file
	 *
	 * @see [MediaStore.Files.FileColumns.MIME_TYPE]
	 */
	abstract val mimeType: String

	/**
	 * the size of the file, in bytes
	 *
	 * @see [MediaStore.Files.FileColumns.SIZE]
	 */
	@StorageDataValue(StorageDataUnit.BYTE)
	abstract val size: Long
}
