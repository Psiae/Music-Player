package com.flammky.android.medialib.temp.media3.contract

import android.os.Bundle
import androidx.media3.common.MediaItem

interface MediaItemFactoryOf<out T: Any> {

	fun createMediaItem(
		from: @UnsafeVariance T,
		metadataExtra: Bundle = Bundle(),
		requestMetadataExtra: Bundle = Bundle()
	): MediaItem
}
