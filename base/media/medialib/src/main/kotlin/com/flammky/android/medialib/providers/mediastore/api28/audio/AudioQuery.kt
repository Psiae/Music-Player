package com.flammky.android.medialib.providers.mediastore.api28.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.api28.MediaStore28
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioQuery

/**
 * class representing an Audio File Query information on MediaStore API 28 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 */

internal data class MediaStoreAudioQuery28 private constructor(
	override val id: Long,
	override val uri: Uri,
	override val albumId: Long?,
	override val artistId: Long?
) : MediaStoreAudioQuery(id, uri, albumId, artistId) {

	class Builder internal constructor() {
		var id: Long = Long.MIN_VALUE
			private set
		var uri: Uri = Uri.EMPTY
			private set
		var artistId: Long? = null
			private set
		var albumId: Long? = null
			private set

		fun setId(id: Long) = apply {
			this.id = id
		}

		fun setUri(uri: Uri) = apply {
			this.uri = uri
		}

		fun setArtistId(artistId: Long?) = apply {
			this.artistId = artistId
		}

		fun setAlbumId(albumId: Long?) = apply {
			this.albumId = albumId
		}

		fun build(): MediaStoreAudioQuery28 {
			return MediaStoreAudioQuery28(
				id = id,
				uri = uri,
				artistId = artistId,
				albumId = albumId
			)
		}
	}

	companion object {
		val empty = Builder().build()
	}
}
