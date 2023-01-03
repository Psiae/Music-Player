package com.flammky.android.medialib.providers.mediastore.api29

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioQuery

class MediaStoreAudioQuery29 private constructor(
	val relativePath: String,
	val ownerPackageName: String,
	val volumeName: String,
	id: Long,
	uri: Uri,
	albumId: Long,
	artistId: Long,
) : MediaStoreAudioQuery(id, uri, albumId, artistId) {

	class Builder internal constructor() {
		var id: Long = Long.MIN_VALUE
		var uri: Uri = Uri.EMPTY
		var albumId: Long = Long.MIN_VALUE
		var artistId: Long = Long.MIN_VALUE
		var relativePath: String = ""
		var ownerPackageName: String = ""
		var version: String = ""
		var volumeName: String = ""

		internal fun build(): MediaStoreAudioQuery29 {
			return MediaStoreAudioQuery29(
				id = id,
				uri = uri,
				albumId = albumId,
				artistId = artistId,
				relativePath = relativePath,
				ownerPackageName = ownerPackageName,
				volumeName = volumeName
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
