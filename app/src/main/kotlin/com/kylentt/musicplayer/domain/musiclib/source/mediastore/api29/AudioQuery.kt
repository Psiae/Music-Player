package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api29

import android.content.ContentResolver
import android.net.Uri
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio.MediaStoreAudioQuery

class MediaStoreAudioQuery29(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long,
	override val artistId: Long,
	override val version: Long,
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
		var version: Long = Long.MIN_VALUE
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
