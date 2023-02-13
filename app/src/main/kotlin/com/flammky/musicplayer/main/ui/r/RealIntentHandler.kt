package com.flammky.musicplayer.main.ui.r

import android.content.Intent
import com.flammky.android.content.intent.isActionView
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.main.ext.IntentHandler
import com.flammky.musicplayer.main.ext.MediaIntentHandler
import timber.log.Timber

class RealIntentHandler(
	// Temp
	private val mediaIntentHandler: MediaIntentHandler
) : IntentHandler {

	private val _stateLock = Any()
	private var _disposed = false
	private val _interceptors = mutableListOf<Interceptor>()
	private val _interceptedIntents = mutableListOf<Intent>()

	override fun createInterceptor(): IntentHandler.Interceptor {
		return Interceptor(this).also { _interceptors.sync { add(it) } }
	}

	override fun handleIntent(intent: Intent) {
		sync(_stateLock) {
			if (_disposed) return
		}

		val clone = intent.clone() as Intent

		for (interceptor in _interceptors.sync { toList() }) {
			val intercepted = interceptor.intercept(
				intent = clone,
				resume = { resumedHandleIntent(clone, interceptor) },
				cancel = { _interceptedIntents.sync { remove(clone) } }
			)
			if (intercepted) {
				_interceptedIntents.sync { add(clone) }
				return
			}
		}

		privateHandleIntent(intent)
	}

	private fun resumedHandleIntent(
		intent: Intent,
		resumingInterceptor: Interceptor
	) {
		Timber.d("resumedHandleIntent: $intent")

		_interceptedIntents.sync { if (!contains(intent)) return else remove(intent) }

		val clone = intent.clone() as Intent

		for (interceptor in _interceptors.sync { filter { it != resumingInterceptor } }) {
			val intercepted = interceptor.intercept(
				intent = clone,
				resume = { resumedHandleIntent(clone, interceptor) },
				cancel = { _interceptedIntents.sync { remove(clone) } }
			)
			if (intercepted) {
				_interceptedIntents.sync { add(clone) }
				return
			}
		}

		privateHandleIntent(intent)
	}

	private fun privateHandleIntent(intent: Intent) {
		if (mediaIntentHandler.isMediaIntent(intent)) {
			mediaIntentHandler.handleMediaIntent(intent)
			return
		}
	}

	override fun intentRequireAndroidPermission(
		intent: Intent,
		permission: AndroidPermission
	): Boolean {
		// TODO
		return intent.isActionView() && intent.type?.startsWith("audio/") == true
	}

	override fun intentRequireAuthPermission(intent: Intent): Boolean {
		return true
	}

	override fun dispose() {
		// temp
		sync(_stateLock) {
			_interceptedIntents.sync {
				clear()
			}
			_interceptors.sync {
				forEach { it.dispose() }
				clear()
			}
		}
	}

	private fun notifyDisposedInterceptor(interceptor: Interceptor) {
		_interceptors.sync { remove(interceptor) }
	}

	private fun notifyStartedInterceptor(interceptor: Interceptor) {
		_interceptors.sync { if (!contains(interceptor)) add(interceptor) }
	}

	private fun notifyStoppedInterceptor(interceptor: IntentHandler.Interceptor) {
		_interceptors.sync { remove(interceptor) }
	}

	private class Interceptor(
		private val parent: RealIntentHandler
	) : IntentHandler.Interceptor {

		private val _interceptedIntents = mutableListOf<InterceptedIntent>()

		@Volatile
		private var _filter: (IntentHandler.TargetIntent) -> Boolean = { false }

		override fun collectInterceptedIntent(): List<InterceptedIntent> {
			return _interceptedIntents.sync { toList() }
		}

		override fun dispatchAllInterceptedIntent() {
			collectInterceptedIntent().forEach { it.resume() }
		}

		override fun dropAllInterceptedIntent() {
			return _interceptedIntents.sync {
				val collect = toList()
				clear()
				collect.forEach { it.cancel() }
			}
		}

		override fun start() {
			parent.notifyStartedInterceptor(this)
		}

		override fun setFilter(filter: (IntentHandler.TargetIntent) -> Boolean) {
			_filter = filter
		}

		override fun dispose() {
			dispatchAllInterceptedIntent()
			parent.notifyDisposedInterceptor(this)
		}

		override fun isParent(parent: IntentHandler): Boolean {
			return this.parent === parent
		}

		fun intercept(
			intent: Intent,
			resume: () -> Unit,
			cancel: () -> Unit
		): Boolean {
			val intercepted = _filter(TargetIntent(intent))

			if (intercepted) {
				_interceptedIntents.sync { add(InterceptedIntent(intent, resume, cancel)) }
			}

			return intercepted
		}

		private class InterceptedIntent(
			private val struct: Intent,
			val resume: () -> Unit,
			val cancel: () -> Unit
		): IntentHandler.InterceptedIntent {
			override fun cloneActual(): Intent = struct.clone() as Intent
		}

		private class TargetIntent(private val actual: Intent): IntentHandler.TargetIntent {
			override fun cloneActual(): Intent = actual.clone() as Intent
		}
	}
}
