package com.flammky.android.medialib.temp.provider.mediastore.base.media

import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreMetadataEntry
import java.util.*
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#391)
 */

@Immutable
abstract class MediaStoreMetadata internal constructor() {

	/**
	 * The TITLE metadata
	 */
	abstract val title: String?

	override fun hashCode(): Int = Objects.hash(title)
	override fun equals(other: Any?): Boolean = other is MediaStoreMetadataEntry && other.title == title
}
