package com.flammky.android.medialib.common.mediaitem

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem.RequestMetadata
import javax.annotation.concurrent.Immutable


@Immutable
open class MediaItem protected constructor(
	val bundle: Bundle?,
	val mediaId: String,
	val mediaUri: Uri,
	val metadata: MediaMetadata
) {

	class Builder @JvmOverloads constructor(
		private var mediaId: String = "",
		private var uri: Uri = Uri.EMPTY
	) {
		private var bundle: Bundle? = null
		private var metadata: MediaMetadata = MediaMetadata.UNSET

		fun setMediaId(mediaId: String) = apply { this.mediaId = mediaId }
		fun setUri(uri: Uri) = apply { this.uri = uri }
		fun setBundle(bundle: Bundle) = apply { this.bundle = bundle }
		fun setMetadata(metadata: MediaMetadata) = apply { this.metadata = metadata }
		fun build(): MediaItem = MediaItem(bundle, mediaId, uri, metadata)
	}

	val media3 by lazy {
		androidx.media3.common.MediaItem.Builder()
			.setUri(mediaUri)
			.setMediaId(mediaId)
			.setMediaMetadata(metadata.media3)
			.setRequestMetadata(RequestMetadata.Builder().setMediaUri(mediaUri).build())
			.build()
	}

	companion object {
		@JvmStatic val UNSET = Builder().build()
	}
}
