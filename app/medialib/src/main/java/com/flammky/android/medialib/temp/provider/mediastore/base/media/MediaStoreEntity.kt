package com.flammky.android.medialib.temp.provider.mediastore.base.media

import android.net.Uri

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java)
 */

abstract class MediaStoreEntity internal constructor() {

	/**
	 * Unique Identifier
	 */
	abstract val uid: String

	/**
	 * The Content [Uri]
	 */
	abstract val uri: Uri

	/**
	 * The File Information.
	 */
	abstract val fileInfo: MediaStoreFile

	/**
	 * The Metadata Information.
	 */
	abstract val metadataInfo: MediaStoreMetadata

	/**
	 * The Query Information, anything that is specific to MediaStore query tables
	 */
	internal abstract val queryInfo: MediaStoreQuery
}
