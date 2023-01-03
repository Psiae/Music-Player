package com.flammky.android.medialib.context.internal

import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.media3.Media3Item

internal class RealLibraryContext(android: AndroidContext) : InternalLibraryContext(android) {

	override val mediaItemBuilder: InternalMediaItem.Builder = Media3Item.Builder("Internal")
}
