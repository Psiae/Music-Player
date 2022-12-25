package com.flammky.musicplayer.main.ui.r

import android.content.Intent
import com.flammky.android.content.intent.isActionView
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.temp.image.internal.TestArtworkProvider
import com.flammky.kotlin.common.sync.sync
import com.flammky.mediaplayer.helper.external.IntentWrapper
import com.flammky.mediaplayer.helper.external.MediaIntentHandler
import com.flammky.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.flammky.musicplayer.app.ApplicationDelegate
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionPlayback
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionRepository
import com.flammky.musicplayer.domain.media.RealMediaConnection
import com.flammky.musicplayer.domain.musiclib.service.MusicLibraryService
import com.flammky.musicplayer.main.ext.IntentHandler
import timber.log.Timber

class RealIntentHandler(
	// Temp
	private val mediaIntentHandler: MediaIntentHandler = run {
		// TODO: remove this mess
		val ctx = ApplicationDelegate.get()
		val deprecatedMusicLib = com.flammky.android.medialib.temp.MediaLibrary.construct(ctx, MusicLibraryService::class.java)
		MediaIntentHandlerImpl(
			artworkProvider = TestArtworkProvider(
				context = ctx,
				lru = deprecatedMusicLib.imageRepository.sharedBitmapLru
			),
			context = ctx,
			dispatcher = AndroidCoroutineDispatchers.DEFAULT,
			mediaSource = deprecatedMusicLib.providers.mediaStore,
			mediaConnection = RealMediaConnection(
				delegate = RealMediaConnectionDelegate(
					MediaLib.singleton(ctx).mediaProviders.mediaStore,
					RealMediaConnectionPlayback(),
					RealMediaConnectionRepository.provide(ctx, AndroidCoroutineDispatchers.DEFAULT)
				)
			),
			MediaLib.singleton(ctx)
		)
	}
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
			val intercepted = interceptor.intercept(clone) {
				resumedHandleIntent(clone, interceptor)
			}
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
			val intercepted = interceptor.intercept(clone) {
				resumedHandleIntent(clone, interceptor)
			}
			if (intercepted) {
				_interceptedIntents.sync { add(clone) }
				return
			}
		}

		privateHandleIntent(intent)
	}

	private fun privateHandleIntent(intent: Intent) {
		val wrap = IntentWrapper(intent)
		mediaIntentHandler.handleMediaIntentI(wrap)
	}

	override fun intentRequireAndroidPermission(
		intent: Intent,
		permission: AndroidPermission
	): Boolean {
		// TODO
		return intent.isActionView() && intent.type?.startsWith("audio/") == true
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
			return _interceptedIntents.sync { toList() }
		}

		override fun start() {
			//
		}

		override fun setFilter(filter: (IntentHandler.TargetIntent) -> Boolean) {
			_filter = filter
		}

		override fun dispose() {
			dispatchAllInterceptedIntent()
			parent.notifyDisposedInterceptor(this)
		}

		fun intercept(
			intent: Intent,
			resume: () -> Unit
		): Boolean {
			val intercepted = _filter(TargetIntent(intent))

			if (intercepted) {
				_interceptedIntents.sync { add(InterceptedIntent(intent, resume)) }
			}

			return intercepted
		}

		private class InterceptedIntent(
			private val struct: Intent,
			val resume: () -> Unit
		): IntentHandler.InterceptedIntent {
			override fun cloneActual(): Intent = struct.clone() as Intent
		}

		private class TargetIntent(private val actual: Intent): IntentHandler.TargetIntent {
			override fun cloneActual(): Intent = actual.clone() as Intent
		}
	}
}
