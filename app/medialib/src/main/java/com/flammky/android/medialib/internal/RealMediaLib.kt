package com.flammky.android.medialib.internal

import android.content.Context
import com.flammky.android.medialib.context.internal.AndroidContext
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.core.internal.RealMediaLibrary
import com.flammky.common.kotlin.lazy.LazyConstructor

internal object RealMediaLib {
	private val singleton = LazyConstructor<MediaLibrary>()

	fun singleton(context: Context): MediaLibrary = singleton.construct {
		RealMediaLibrary(AndroidContext(context))
	}
}
