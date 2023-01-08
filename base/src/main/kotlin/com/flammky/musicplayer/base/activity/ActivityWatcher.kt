package com.flammky.musicplayer.base.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import timber.log.Timber

@MainThread
class ActivityWatcher private constructor(
	private val app: Application
) {
	private val _activities = mutableListOf<Activity>()

	private val listener = object : Application.ActivityLifecycleCallbacks {
		override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
			Timber.d("ActivityLifecycleCallback, onActivityPreCreated: activity=$activity ; bundle=$savedInstanceState")
			assertThreadAccessFromCallback()
			_activities.add(activity)
		}
		override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityCreated: activity=$activity ; bundle=$savedInstanceState")
		}
		override fun onActivityPreStarted(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPreStarted: activity=$activity")
		}
		override fun onActivityStarted(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityStarted: activity=$activity")
		}
		override fun onActivityPreResumed(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPreResumed: activity=$activity")
		}
		override fun onActivityResumed(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityResumed: activity=$activity")
		}
		override fun onActivityPrePaused(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPrePaused: activity=$activity")
		}
		override fun onActivityPaused(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPaused: activity=$activity")
		}
		override fun onActivityPreStopped(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPreStopped: activity=$activity")
		}
		override fun onActivityStopped(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityStopped: activity=$activity")
		}
		override fun onActivityPreSaveInstanceState(activity: Activity, outState: Bundle) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onPreSaveInstanceState: activity=$activity ; bundle:$outState")
		}
		override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivitySaveInstanceState: activity=$activity ; bundle:$outState")
		}
		override fun onActivityPreDestroyed(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityPreDestroyed: activity=$activity")
		}
		override fun onActivityDestroyed(activity: Activity) {
			assertThreadAccessFromCallback()
			Timber.d("ActivityLifecycleCallback, onActivityDestroyed: activity=$activity")
			_activities.remove(activity)
		}
	}

	init {
		app.registerActivityLifecycleCallbacks(listener)
	}

	//
	// Activity instance should Not be leaked to outside
	//

	fun count(): Int {
		assertThreadAccess()
		return _activities.size
	}

	fun hasActivity(cls: Class<out Activity>): Boolean {
		assertThreadAccess()
		return _activities.any { it::class.java == cls }
	}

	fun registerOnPreResumeCallback(
		callback: () -> Unit
	) {
		assertThreadAccess()

	}

	fun registerOnResumeCallback(
		callback: () -> Unit
	) {

	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun assertThreadAccess() {
		if (Thread.currentThread() == Looper.getMainLooper().thread) {
			return
		}
		wrongThreadAccess()
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun assertThreadAccessFromCallback() {
		if (Thread.currentThread() == Looper.getMainLooper().thread) {
			return
		}
		// though I doubt this will somehow happen as the abstract implementation already ensure this
		wrongThreadAccessFromCallback()
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun wrongThreadAccess(): Nothing {
		// first visible callback is from `performCreate` there's earlier execution point
		error("ActivityWatcher must be accessed on the `Main Thread`, for reliability reason")
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun wrongThreadAccessFromCallback(): Nothing {
		error("ActivityWatcher received lifecycle callback from other thread than `Main Thread`")
	}

	companion object {
		private val constructor = LazyConstructor<ActivityWatcher>()

		internal infix fun provides(application: Application) =
			constructor.construct { ActivityWatcher(application) }

		fun get(): ActivityWatcher = constructor.valueOrNull()
			?: error("ActivityWatcher was not initialized")
	}
}
