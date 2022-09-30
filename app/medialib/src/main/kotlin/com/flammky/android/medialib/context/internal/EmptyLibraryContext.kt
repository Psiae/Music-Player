package com.flammky.android.medialib.context.internal

import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.context.AndroidContext

internal class EmptyLibraryContext : InternalLibraryContext() {

	override val android: AndroidContext
		get() = TODO("Not yet implemented")

	override val mediaItemBuilder =
		InternalMediaItem.Builder { _, _, _, _ -> InternalMediaItem.UNSET }
}
