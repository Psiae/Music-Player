package com.flammky.android.medialib.context

import android.app.Application
import android.content.Context
import android.content.ContextWrapper

class AndroidContext internal constructor(context: Context): ContextWrapper(context) {
	val application: Application = context.applicationContext as Application
}
