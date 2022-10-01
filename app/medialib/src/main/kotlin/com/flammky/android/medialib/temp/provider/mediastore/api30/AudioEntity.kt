package com.flammky.android.medialib.temp.provider.mediastore.api30

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore30
 */
class MediaStoreAudioEntity30 private constructor(
	override val uid: String,
	override val uri: Uri,
	override val fileInfo: MediaStoreAudioFile30,
	override val metadataInfo: MediaStoreAudioMetadata30,
	internal override val queryInfo: MediaStoreAudioQuery30
) : MediaStoreAudioEntity() {

	class Builder internal constructor() {
		var uid: String = ""
		var uri: Uri = Uri.EMPTY
		var fileInfo: MediaStoreAudioFile30 = MediaStoreAudioFile30.empty
		var metadataInfo: MediaStoreAudioMetadata30 = MediaStoreAudioMetadata30.empty
		var queryInfo: MediaStoreAudioQuery30 = MediaStoreAudioQuery30.empty

		internal fun build(): MediaStoreAudioEntity30 {
			return MediaStoreAudioEntity30(
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
