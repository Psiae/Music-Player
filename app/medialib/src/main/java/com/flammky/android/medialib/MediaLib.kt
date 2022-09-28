package com.flammky.android.medialib

import android.content.Context
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.internal.RealMediaLib

object MediaLib {
	private val delegate = RealMediaLib

	val singleton: MediaLibrary
		get() {
			return delegate.singleton
				?: error(
					"""
					MediaLib singleton was not instantiated, did you forgot to call
					MediaLib.singleton(context: Context) ?
					"""
				)
		}

	fun singleton(context: Context): MediaLibrary = delegate.singleton(context)
}
