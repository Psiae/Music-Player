package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle

interface MediaItemFactoryOf<out T: Any> {
    fun createMediaItem(source: @UnsafeVariance T, metadataBundle: Bundle?): MediaItem
}
