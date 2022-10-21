package com.flammky.android.medialib.providers.mediastore.api28.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity

/**
 * class representing an Audio Entity
 * @see MediaStore28
 */
data class MediaStoreAudioEntity28 private constructor(
    override val uid: String,
    override val uri: Uri,
    override val file: MediaStoreAudioFile28,
    override val metadata: MediaStoreAudioMetadataEntry28Entry,
    internal override val queryInfo: MediaStoreAudioQuery28,
) : MediaStoreAudioEntity(uid, uri, file, metadata, queryInfo) {

	class Builder internal constructor() {
		var uid: String = ""
			private set
		var uri: Uri = Uri.EMPTY
			private set
		var file: MediaStoreAudioFile28 = MediaStoreAudioFile28.empty
			private set
		var metadata: MediaStoreAudioMetadataEntry28Entry = MediaStoreAudioMetadataEntry28Entry.empty
			private set
		internal var queryInfo: MediaStoreAudioQuery28 = MediaStoreAudioQuery28.empty
			private set

		fun setUID(uid: String) = apply { this.uid = uid }
		fun setUri(uri: Uri) = apply { this.uri = uri }
		fun setFile(file: MediaStoreAudioFile28) = apply { this.file = file }
		fun setMetadata(metadata: MediaStoreAudioMetadataEntry28Entry) = apply { this.metadata = metadata }
		internal fun setQueryInfo(queryInfo: MediaStoreAudioQuery28) = apply { this.queryInfo = queryInfo }

		internal fun build(): MediaStoreAudioEntity28 {
			return MediaStoreAudioEntity28(
				uid = uid,
				uri = uri,
				file = file,
				metadata = metadata,
				queryInfo = queryInfo
			)
		}

	}

	companion object {
		val empty = Builder().build()
		internal fun build(apply: Builder.() -> Unit): MediaStoreAudioEntity28 {
			return Builder().apply(apply).build()
		}
	}
}
