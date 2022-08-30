package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import android.net.Uri
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio.MediaStoreAudioQuery

/**
 * class representing an Audio File Query information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 */

class MediaStoreAudioQuery28 private constructor(
	override val id: Long,
	override val uri: Uri
) : MediaStoreAudioQuery() {

	class Builder internal constructor() {
		var id: Long = Long.MIN_VALUE
		var uri: Uri = Uri.EMPTY

		internal fun build(): MediaStoreAudioQuery28 {
			return MediaStoreAudioQuery28(id, uri)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
