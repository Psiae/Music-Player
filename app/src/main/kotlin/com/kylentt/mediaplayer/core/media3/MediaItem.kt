package com.kylentt.mediaplayer.core.media3

import android.net.Uri
import androidx.media3.common.MediaItem

object MediaItemFactory {

	val EMPTY
		get() = MediaItem.EMPTY

	fun newBuilder(): MediaItem.Builder = MediaItem.Builder()

	fun fillInLocalConfig(item: MediaItem, itemUri: Uri) = fillInLocalConfig(item, itemUri.toString())

	fun fillInLocalConfig(item: MediaItem, itemUri: String): MediaItem = with(newBuilder()) {
		setMediaMetadata(item.mediaMetadata)
		setMediaId(item.mediaId)
		setUri(itemUri)
		build()
	}
}
