package com.flammky.musicplayer.main.ext

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


	interface Interceptor {
		fun start()
	}

	interface InterceptedIntent {

	}
}
