package com.kylentt.musicplayer.domain.musiclib.core.internal

import android.app.Application
import android.content.Context

internal class ComponentModule private constructor() {
	private var mApplication: Application? = null
	private var mContext: Context? = null

	internal fun attachContext(context: Context) {
		check(mContext == null) {
			"Invalid Internal usage, ComponentModule Context was already initialized"
		}
		check(mApplication == null) {
			"Invalid Internal usage, ComponentModule Application was already initialized"
		}
		mContext = context
		mApplication = context.applicationContext as Application
	}

	companion object {
		internal val instance: ComponentModule = ComponentModule()

		internal fun requireApplication(): Application {
			val application = instance.mApplication
			requireNotNull(application) {
				"Invalid Internal usage, ComponentModule Application was null"
			}
			return application
		}

		internal fun requireContext(): Context {
			val context = instance.mContext
			requireNotNull(context) {
				"Invalid Internal usage, ComponentModule Context was null"
			}
			return context
		}

	}
}
