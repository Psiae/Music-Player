package com.flammky.android.medialib.context.internal

import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.LibraryContext

internal abstract class InternalLibraryContext(android: AndroidContext) : LibraryContext(android) {
	abstract val mediaItemBuilder: InternalMediaItem.Builder
}
