package com.flammky.android.medialib.core.internal

import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.core.MediaLibrary

internal class RealMediaLibrary(
	androidContext: AndroidContext
) : MediaLibrary {

	override val context: LibraryContext
		get() = TODO("Not yet implemented")
}
