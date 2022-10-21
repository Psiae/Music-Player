package com.flammky.android.medialib.providers.mediastore.api30

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore30
 */
class MediaStoreAudioEntity30 private constructor(
    uid: String,
    uri: Uri,
    override val file: MediaStoreAudioFile30,
    override val metadata: MediaStoreAudioMetadataEntry30Entry,
    internal override val queryInfo: MediaStoreAudioQuery30
) : MediaStoreAudioEntity(uid, uri, file, metadata, queryInfo) {

	class Builder internal constructor() {
		var uid: String = ""
		var uri: Uri = Uri.EMPTY
		var fileInfo: MediaStoreAudioFile30 = MediaStoreAudioFile30.empty
		var metadataInfo: MediaStoreAudioMetadataEntry30Entry = MediaStoreAudioMetadataEntry30Entry.empty
		var queryInfo: MediaStoreAudioQuery30 = MediaStoreAudioQuery30.empty

		internal fun build(): MediaStoreAudioEntity30 {
			return MediaStoreAudioEntity30(
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
