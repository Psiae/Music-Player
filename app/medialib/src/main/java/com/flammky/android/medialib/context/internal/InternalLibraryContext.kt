package com.flammky.android.medialib.context.internal

import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.context.LibraryContext

internal abstract class InternalLibraryContext : LibraryContext()  {
	abstract val mediaItemBuilder: InternalMediaItem.Builder

	override val internal: InternalLibraryContext
		get() {
			return this
		}
}
