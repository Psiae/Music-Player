package com.flammky.android.medialib.providers.mediastore.base.audio

import android.provider.MediaStore
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreFile
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
@Immutable
abstract class MediaStoreAudioFile internal constructor(
	override val absolutePath: String?,
	override val dateAdded: Long?,
	override val dateModified: Long?,
	override val fileName: String?,
	override val mimeType: String?,
	override val size: Long?
) : MediaStoreFile(
	absolutePath,
	dateAdded,
	dateModified,
	fileName,
	MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO,
	mimeType,
	size
)
