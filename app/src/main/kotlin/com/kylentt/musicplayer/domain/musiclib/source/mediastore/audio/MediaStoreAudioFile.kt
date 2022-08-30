package com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28.MediaStore28
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.media.MediaStoreFile

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
abstract class MediaStoreAudioFile internal constructor() : MediaStoreFile() {
	override val mediaType: Int = MediaStore28.Files.FileColumns.MEDIA_TYPE_AUDIO
}
