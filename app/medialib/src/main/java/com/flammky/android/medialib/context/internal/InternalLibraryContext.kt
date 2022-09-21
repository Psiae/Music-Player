package com.flammky.android.medialib.context.internal

import com.flammky.android.medialib.common.mediaitem.InternalMediaItem

internal interface InternalLibraryContext {
	val mediaItemBuilder: InternalMediaItem.Builder

	object UNSET : InternalLibraryContext {
		override val mediaItemBuilder =
			InternalMediaItem.Builder { _, _, _, _ -> InternalMediaItem.UNSET }
	}
}
