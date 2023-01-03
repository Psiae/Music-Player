package com.flammky.android.medialib.providers.mediastore.base.media

import android.provider.MediaStore
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#471)
 *
 * representation of available File information within MediaStore API
 */

@Immutable
abstract class MediaStoreFile internal constructor(

	/**
	 * The `absolute path` of the File. null if not present
	 *
	 * @see [MediaStore.Files.FileColumns.DATA]
	 */
	@JvmField
	val absolutePath: String?,

	/**
	 * The `date-added` of this file.
	 * The value represent elapsed seconds since Unix Epoch Time.
	 *
	 * @see [MediaStore.Files.FileColumns.DATE_ADDED]
	 */
	@JvmField
	@DurationValue(TimeUnit.SECONDS)
	val dateAdded: Long?,

	/**
	 * The `date-modified` of this file.
	 * The value represent elapsed [TimeUnit.SECONDS] since Unix Epoch Time.
	 *
	 * @see [MediaStore.Files.FileColumns.DATE_MODIFIED]
	 */
	@JvmField
	@DurationValue(TimeUnit.SECONDS)
	val dateModified: Long?,

	/**
	 * The name of the file including its extension
	 *
	 * @see [MediaStore.Files.FileColumns.DISPLAY_NAME]
	 */
	@JvmField
	val fileName: String?,

	/**
	 * The media type (audio, video, image or playlist) of the file,
	 * or 0 for not a media file
	 *
	 * @see [MediaStore.Files.FileColumns.MEDIA_TYPE]
	 */
	@JvmField
	val mediaType: Int?,

	/**
	 * the Mime-Type of the file
	 *
	 * @see [MediaStore.Files.FileColumns.MIME_TYPE]
	 */
	@JvmField
	val mimeType: String?,

	/**
	 * the size of the file, in bytes
	 *
	 * @see [MediaStore.Files.FileColumns.SIZE]
	 */
	@JvmField
	val size: Long?
)
