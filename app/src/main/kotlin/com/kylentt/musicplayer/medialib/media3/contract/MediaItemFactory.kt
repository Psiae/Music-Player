package com.kylentt.musicplayer.medialib.media3.contract

import android.os.Bundle
import androidx.media3.common.MediaItem

internal interface MediaItemFactoryOf<T> {

	fun createMediaItem(
		from: T,
		metadataExtra: Bundle = Bundle(),
		requestMetadataExtra: Bundle = Bundle()
	): MediaItem
}
