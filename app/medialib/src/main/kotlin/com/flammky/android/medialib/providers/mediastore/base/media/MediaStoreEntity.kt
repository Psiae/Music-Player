package com.flammky.android.medialib.providers.mediastore.base.media

import android.net.Uri
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java)
 *
 * representation of Media Files within MediaStore API, composed of :
 * + [MediaStoreFile]
 * + [MediaStoreMetadataEntry]
 * + [MediaStoreQuery]
 */

@Immutable
abstract class MediaStoreEntity internal constructor(
    /** The Unique Identifier */
	open val uid: String,

    /** The [Uri], always content scheme */
	open val uri: Uri,

    /** The File Information. */
	open val file: MediaStoreFile,

    /** The Metadata Information. */
	open val metadata: MediaStoreMetadataEntry,

    /** The Query Information, anything that is specific to MediaStore query tables. */
	internal open val queryInfo: MediaStoreQuery
)
