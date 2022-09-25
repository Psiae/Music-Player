package com.flammky.android.medialib

import android.content.Context
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.internal.RealMediaLib

object MediaLib {
	fun singleton(context: Context): MediaLibrary = RealMediaLib.singleton(context)
}
