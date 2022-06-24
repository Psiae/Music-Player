package com.kylentt.mediaplayer.core.media3.mediaitem

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.media3.MediaItemFactory
import com.kylentt.mediaplayer.helper.Preconditions
import javax.inject.Singleton

@Singleton
class MediaItemHelper(
	private val context: Context
) {

	fun buildFromMetadata(uri: Uri): MediaItem = MediaItemFactory.fromMetaData(context, uri)

	@Suppress("NOTHING_TO_INLINE")
	inline fun rebuildMediaItem(item: MediaItem): MediaItem {
		return MediaItem.Builder()
			.setMediaId(item.mediaId)
			.setUri(item.mediaMetadata.mediaUri)
			.setMediaMetadata(item.mediaMetadata)
			.build()
	}

	fun getEmbeddedPicture(item: MediaItem): ByteArray? =
		item.mediaMetadata.mediaUri?.let { getEmbeddedPicture(it) }

	fun getEmbeddedPicture(uri: Uri): ByteArray? = MediaItemFactory.getEmbeddedImage(context, uri)

	init {
		Preconditions.checkArgument(context is Application)
	}
}
