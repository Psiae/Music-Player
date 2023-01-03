package com.flammky.android.medialib.common.mediaitem

import android.net.Uri

internal abstract class InternalMediaItem {

	internal fun interface Builder {
		fun build(extra: MediaItem.Extra, mediaId: String, uri: Uri, metadata: MediaMetadata): InternalMediaItem
	}

	object UNSET : InternalMediaItem()
}

