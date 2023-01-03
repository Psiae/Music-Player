package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import android.provider.MediaStore
import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaStoreFile

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
abstract class MediaStoreAudioFile internal constructor() : MediaStoreFile() {
	override val mediaType: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
}
