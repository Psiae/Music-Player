package com.flammky.android.medialib.temp.common.intent

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

class AndroidCommonIntent(private val context: Context) {

	val appDetailSetting: Intent by lazy {
		Intent().apply {
			action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
			data = "package:${context.packageName}".toUri()
		}
	}
}
