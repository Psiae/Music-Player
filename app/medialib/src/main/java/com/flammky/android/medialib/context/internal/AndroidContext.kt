package com.flammky.android.medialib.context.internal

import android.app.Application
import android.content.Context
import android.content.ContextWrapper

internal class AndroidContext(context: Context): ContextWrapper(context) {
	val application: Application = context.applicationContext as Application
}
