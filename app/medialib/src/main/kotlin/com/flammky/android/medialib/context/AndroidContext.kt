package com.flammky.android.medialib.context

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.ContextWrapper

class AndroidContext internal constructor(private val context: Context): ContextWrapper(context) {

	val application: Application
		get() = context.applicationContext as Application

	companion object {
		@SuppressLint("StaticFieldLeak")
		val EMPTY = AndroidContext(InvalidAndroidContext)
	}
}
