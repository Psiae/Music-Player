package com.flammky.android.medialib.providers.mediastore.base.media

import java.util.*
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#391)
 *
 * representation of available Media File Metadata information within MediaStore API
 */

@Immutable
abstract class MediaStoreMetadata internal constructor(
	/**
	 * The TITLE metadata
	 */
	open val title: String?
) {
	override fun hashCode(): Int = Objects.hash(title)
	override fun equals(other: Any?): Boolean = other is MediaStoreMetadata && other.title == title
}
