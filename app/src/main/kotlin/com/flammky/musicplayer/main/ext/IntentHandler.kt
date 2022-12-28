package com.flammky.musicplayer.main.ext

import android.content.Intent
import com.flammky.android.manifest.permission.AndroidPermission

/**
 * Intent Handler interface for the `Main` Module
 */
interface IntentHandler {

	/**
	 * Create an Interceptor to intercept incoming Intent sent to this handler.
	 *
	 * Interceptor callbacks between others are called in order they are requested
	 *
	 * if an intent is intercepted and the intercepted intent is resumed it will also be delegated to
	 * other interceptors
	 */
	fun createInterceptor(): Interceptor

	fun handleIntent(intent: Intent)

	fun intentRequireAndroidPermission(
		intent: Intent,
		permission: AndroidPermission
	): Boolean

	fun intentRequireAuthPermission(
		intent: Intent
	): Boolean

	fun dispose()

	interface Interceptor {
		fun collectInterceptedIntent(): List<InterceptedIntent>
		fun dispatchAllInterceptedIntent()
		fun dropAllInterceptedIntent()
		fun start()
		fun setFilter(filter: (TargetIntent) -> Boolean)

		/**
		 * Dispose the interceptor, if there's an intercepted intent then it will be dispatched
		 */
		fun dispose()
	}

	interface TargetIntent {
		fun cloneActual(): Intent
	}

	interface InterceptedIntent {
		fun cloneActual(): Intent
	}
}
