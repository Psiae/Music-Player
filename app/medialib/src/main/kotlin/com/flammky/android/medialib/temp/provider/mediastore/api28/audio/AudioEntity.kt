package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore28
 */
class MediaStoreAudioEntity28 private constructor(
	override val uid: String,
	override val uri: Uri,
	override val fileInfo: MediaStoreAudioFile28,
	override val metadataInfo: MediaStoreAudioMetadata28,
	override val queryInfo: MediaStoreAudioQuery28,
) : MediaStoreAudioEntity() {

	class Builder internal constructor() {
		var uid: String = ""
		var uri: Uri = Uri.EMPTY
		var fileInfo: MediaStoreAudioFile28 = MediaStoreAudioFile28.empty
		var metadataInfo: MediaStoreAudioMetadata28 = MediaStoreAudioMetadata28.empty
		var queryInfo: MediaStoreAudioQuery28 = MediaStoreAudioQuery28.empty

		internal fun build(): MediaStoreAudioEntity28 {
			return MediaStoreAudioEntity28(
				uid = uid,
				uri = uri,
				fileInfo = fileInfo,
				metadataInfo = metadataInfo,
				queryInfo = queryInfo
			)
		}

	}

	companion object {
		val empty =
			Builder()
				.build()
	}
}
