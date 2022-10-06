package com.flammky.android.medialib.providers.mediastore.api29

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore29
 */
class MediaStoreAudioEntity29 private constructor(
	uid: String,
	uri: Uri,
	override val file: MediaStoreAudioFile29,
	override val metadata: MediaStoreAudioMetadata29,
	internal override val queryInfo: MediaStoreAudioQuery29
) : MediaStoreAudioEntity(uid, uri, file, metadata, queryInfo) {

	class Builder internal constructor() {
		var uid: String = ""
		var uri: Uri = Uri.EMPTY
		var fileInfo: MediaStoreAudioFile29 = MediaStoreAudioFile29.empty
		var metadataInfo: MediaStoreAudioMetadata29 = MediaStoreAudioMetadata29.empty
		var queryInfo: MediaStoreAudioQuery29 = MediaStoreAudioQuery29.empty

		internal fun build(): MediaStoreAudioEntity29 {
			return MediaStoreAudioEntity29(
				uid = uid,
				uri = uri,
				file = fileInfo,
				metadata = metadataInfo,
				queryInfo = queryInfo
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
