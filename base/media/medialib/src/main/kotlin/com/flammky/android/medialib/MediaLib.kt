package com.flammky.android.medialib

import android.content.Context
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.internal.RealMediaLib
import com.flammky.androidx.content.context.findActivity
import com.flammky.androidx.content.context.findService

@Deprecated("To Be Rewritten")
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

	fun singleton(context: Context): MediaLibrary {
		// Leak Protection
		require(context.findActivity() == null) {
			"Leaking Activity in MediaLib.singleton(Context)"
		}
		require(context.findService() == null) {
			"Leaking Service in MediaLib.singleton(Context)"
		}
		return delegate.singleton(context)
	}
}
