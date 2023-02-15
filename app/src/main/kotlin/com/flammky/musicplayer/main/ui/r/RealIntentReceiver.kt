package com.flammky.musicplayer.main.ui.r

import android.content.Intent
import com.flammky.android.content.intent.isActionView
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.main.ext.IntentReceiver
import com.flammky.musicplayer.main.ext.MediaIntentHandler

class RealIntentReceiver(
	// Temp
	private val mediaIntentHandler: MediaIntentHandler
) : IntentReceiver {

	private val _stateLock = Any()
	private var _disposed = false
	private val _interceptors = mutableListOf<Interceptor>()
	private val _interceptedIntents = mutableListOf<Intent>()
	private val _pendingIntents = mutableListOf<Intent>()

	override fun createInterceptor(): IntentReceiver.Interceptor {
		return Interceptor(this).also { _interceptors.sync { add(it) } }
	}

	override fun sendIntent(intent: Intent) {
		sync(_stateLock) {
			if (_disposed) return
		}

		val clone = intent.clone() as Intent

		_interceptors.sync {
			for (interceptor in this) {
				val intercepted = interceptor.intercept(
					intent = clone,
					resume = {
						resumedHandleIntent(clone, listOf(interceptor))
					 },
					cancel = {
						_interceptedIntents.sync { remove(clone) }
					},
					pending = {
						_interceptedIntents.sync { remove(clone) }
						_pendingIntents.sync { add(clone) }
					}
				)
				if (intercepted) {
					_interceptedIntents.sync { add(clone) }
					return
				}
			}
		}

		// should give info that no handler intercept the intent, which probably is a bug
		privateHandleIntent(intent = intent)
	}

	private fun resumedHandleIntent(
		intent: Intent,
		resumingInterceptors: List<Interceptor>
	) {
		_interceptedIntents.sync { if (!contains(intent)) return else remove(intent) }

		for (interceptor in _interceptors.sync { filter { it !in resumingInterceptors } }) {
			val intercepted = interceptor.intercept(
				intent = intent,
				resume = {
					resumedHandleIntent(intent, resumingInterceptors.toMutableList().apply { add(interceptor) })
				},
				cancel = {
					_interceptedIntents.sync { remove(intent) }
				},
				pending = {
					_interceptedIntents.sync { remove(intent) }
					_pendingIntents.sync { add(intent) }
				}
			)
			if (intercepted) {
				_interceptedIntents.sync { add(intent) }
				return
			}
		}

		privateHandleIntent(intent = intent)
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
		_interceptors.sync {
			if (contains(interceptor)) {
				val indexToRemove = mutableListOf<Int>()
				_pendingIntents.sync {
					forEachIndexed { index: Int, intent: Intent ->
						val intercepted = interceptor.intercept(
							intent = intent,
							resume = { resumedHandleIntent(intent, listOf(interceptor)) },
							cancel = { _interceptedIntents.sync { remove(intent) } },
							pending = {
								_interceptedIntents.sync { remove(intent) }
								_pendingIntents.sync { add(intent) }
							}
						)
						if (intercepted) {
							_interceptedIntents.sync { add(intent) }
							indexToRemove.add(index)
							return
						}
					}
					indexToRemove.forEach { removeAt(it) }
				}
			}
		}
	}

	private fun notifyStoppedInterceptor(interceptor: IntentReceiver.Interceptor) {
		_interceptors.sync { remove(interceptor) }
	}

	private class Interceptor(
		private val parent: RealIntentReceiver
	) : IntentReceiver.Interceptor {

		private val _interceptedIntents = mutableListOf<InterceptedIntent>()

		@Volatile
		private var _filter: (IntentReceiver.TargetIntent) -> Boolean = { false }

		private var _started = false

		override fun collectInterceptedIntent(): List<InterceptedIntent> {
			return _interceptedIntents.sync { toList() }
		}

		override fun dispatchAllInterceptedIntent() {
			return _interceptedIntents.sync {
				val collect = toList()
				clear()
				collect
			}.forEach { it.resume() }
		}

		override fun dropAllInterceptedIntent() {
			return _interceptedIntents.sync {
				val collect = toList()
				clear()
				collect
			}.forEach { it.cancel() }
		}

		override fun pendingAllInterceptedIntent() {
			return _interceptedIntents.sync {
				val collect = toList()
				clear()
				collect
			}.forEach {
				it.pending()
			}
		}

		override fun start() {
			if (!_started) {
				_started = true
				parent.notifyStartedInterceptor(this)
			}
		}

		override fun setFilter(filter: (IntentReceiver.TargetIntent) -> Boolean) {
			_filter = filter
		}

		override fun dispose() {
			dispatchAllInterceptedIntent()
			parent.notifyDisposedInterceptor(this)
		}

		override fun isParent(parent: IntentReceiver): Boolean {
			return this.parent === parent
		}

		fun intercept(
			intent: Intent,
			resume: () -> Unit,
			cancel: () -> Unit,
			pending: () -> Unit
		): Boolean {
			val intercepted = _filter(TargetIntent(intent))

			if (intercepted) {
				_interceptedIntents.sync { add(InterceptedIntent(intent, resume, cancel, pending)) }
			}

			return intercepted
		}

		private class InterceptedIntent(
			private val struct: Intent,
			val resume: () -> Unit,
			val cancel: () -> Unit,
			val pending: () -> Unit
		): IntentReceiver.InterceptedIntent {
			override fun cloneActual(): Intent = struct.clone() as Intent
		}

		private class TargetIntent(private val actual: Intent): IntentReceiver.TargetIntent {
			override fun cloneActual(): Intent = actual.clone() as Intent
		}
	}
}
