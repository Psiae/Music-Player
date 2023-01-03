package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import android.net.Uri
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioQuery

/**
 * class representing an Audio File Query information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 */

class MediaStoreAudioQuery28 private constructor(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long?,
	override val artistId: Long?,
	override val version: String
) : MediaStoreAudioQuery() {

	class Builder internal constructor() {
		var id: Long = Long.MIN_VALUE
		var uri: Uri = Uri.EMPTY
		var artistId: Long? = null
		var albumId: Long? = null
		var version: String = ""

		internal fun build(): MediaStoreAudioQuery28 {
			return MediaStoreAudioQuery28(
				id = id,
				uri = uri,
				artistId = artistId,
				albumId = albumId,
				version = version
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
