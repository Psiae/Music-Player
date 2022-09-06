package com.kylentt.musicplayer.medialib.internal.provider.mediastore.api28.audio

import android.net.Uri
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.api28.MediaStore28
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.audio.MediaStoreAudioQuery

/**
 * class representing an Audio File Query information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 */

class MediaStoreAudioQuery28 private constructor(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long,
	override val artistId: Long,
	override val version: String
) : MediaStoreAudioQuery() {

	class Builder internal constructor() {
		var id: Long = Long.MIN_VALUE
		var uri: Uri = Uri.EMPTY
		var artistId: Long = Long.MIN_VALUE
		var albumId: Long = Long.MIN_VALUE
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
