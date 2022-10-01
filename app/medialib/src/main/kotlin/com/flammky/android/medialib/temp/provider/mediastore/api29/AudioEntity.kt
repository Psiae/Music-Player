package com.flammky.android.medialib.temp.provider.mediastore.api29

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore29
 */
class MediaStoreAudioEntity29 private constructor(
	override val uid: String,
	override val uri: Uri,
	override val fileInfo: MediaStoreAudioFile29,
	override val metadataInfo: MediaStoreAudioMetadata29,
	internal override val queryInfo: MediaStoreAudioQuery29
) : MediaStoreAudioEntity() {

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
				fileInfo = fileInfo,
				metadataInfo = metadataInfo,
				queryInfo = queryInfo
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
