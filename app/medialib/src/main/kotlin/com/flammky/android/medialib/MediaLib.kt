package com.flammky.android.medialib

import android.content.Context
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.internal.RealMediaLib

object MediaLib {
	private val delegate = RealMediaLib

	val singleton: MediaLibrary
		get() {
			return singletonOrNull
				?: error(
					"""
					MediaLib singleton was not instantiated, did you forgot to call
					MediaLib.singleton(Context) ?
					"""
				)
		}

	val singletonOrNull: MediaLibrary?
		get() {
			return delegate.singleton
		}

	fun singleton(context: Context): MediaLibrary = delegate.singleton(context)
}
