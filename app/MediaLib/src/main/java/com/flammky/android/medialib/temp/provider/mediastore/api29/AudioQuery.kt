package com.flammky.android.medialib.temp.provider.mediastore.api29

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioQuery

class MediaStoreAudioQuery29 private constructor(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long,
	override val artistId: Long,
	override val version: String,
	val relativePath: String,
	val ownerPackageName: String,
	val volumeName: String
) : MediaStoreAudioQuery() {

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
				version = version,
				volumeName = volumeName
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
