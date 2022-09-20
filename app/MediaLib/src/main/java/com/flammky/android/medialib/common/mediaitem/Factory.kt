package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle

interface MediaItemFactoryOf<out T: Any> {

    fun createMediaItem(
        source: @UnsafeVariance T,
        itemBundle: Bundle?,
        metadataBundle: Bundle?
    ): MediaItem

    fun createMediaItem(
        source: @UnsafeVariance T
    ): MediaItem = createMediaItem(source, null, null)
}