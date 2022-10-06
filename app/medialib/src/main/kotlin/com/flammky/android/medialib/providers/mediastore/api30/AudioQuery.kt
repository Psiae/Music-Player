package com.flammky.android.medialib.providers.mediastore.api30

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioQuery

class MediaStoreAudioQuery30 private constructor(
	val relativePath: String,
	val ownerPackageName: String,
	val generationAdded: Long,
	val generationModified: Long,
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
		var version: String = ""
		var relativePath: String = ""
		var ownerPackageName: String = ""
		var generationAdded: Long = Long.MIN_VALUE
		var generationModified: Long = Long.MIN_VALUE
		var volumeName: String = ""

		internal fun build(): MediaStoreAudioQuery30 {
			return MediaStoreAudioQuery30(
				id = id,
				uri = uri,
				albumId = albumId,
				artistId = artistId,
				relativePath = relativePath,
				ownerPackageName = ownerPackageName,
				generationAdded = generationAdded,
				generationModified = generationModified,
				volumeName = volumeName,
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
