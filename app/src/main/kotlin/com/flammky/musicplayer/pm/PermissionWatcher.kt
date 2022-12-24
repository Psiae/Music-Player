package com.flammky.musicplayer.pm

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.android.content.context.ContextHelper
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.activity.ActivityWatcher

// consider whether to let this observe `onResume` callback or make every permission request call to
// give report
class PermissionWatcher private constructor(
	private val app: Application
) {

	private val contextHelper = ContextHelper(app)
	private val mainHandler = Handler(Looper.getMainLooper())
	private val permissionListeners = mutableMapOf<String, List<PermissionListener>>()

	init {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			registerOnPreResumeCallback()
		} else {
			mainHandler.post { registerOnPreResumeCallback() }
		}
	}

	@MainThread
	private fun registerOnPreResumeCallback() {
		val activityWatcher = ActivityWatcher.provides(app)
		activityWatcher.registerOnPreResumeCallback(::onPreResumeCallback)
	}

	private fun onPreResumeCallback() {
	}

	private class PermissionListener {
		fun onPermissionGranted(str: String) {}
	}

	companion object {
		private val constructor = LazyConstructor<PermissionWatcher>()

		infix fun provides(app: Application): PermissionWatcher = constructor.construct {
			PermissionWatcher(app)
		}

		fun get(): PermissionWatcher = constructor.valueOrNull()
			?: error("${PermissionWatcher::class.qualifiedName} was not Initialized")
	}
}
