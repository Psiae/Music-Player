package com.flammky.android.medialib.temp.provider.mediastore.api30

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioQuery

class MediaStoreAudioQuery30 private constructor(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long,
	override val artistId: Long,
	override val version: String,
	val relativePath: String,
	val ownerPackageName: String,
	val generationAdded: Long,
	val generationModified: Long,
	val volumeName: String,
) : MediaStoreAudioQuery() {

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
				version = version,
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
