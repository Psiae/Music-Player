package com.flammky.musicplayer.main.ui.r

import android.content.Context
import androidx.annotation.GuardedBy
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionRepository
import com.flammky.musicplayer.main.ext.IntentHandler
import com.flammky.musicplayer.main.ext.MediaIntentHandler
import com.flammky.musicplayer.main.ui.MainPresenter
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RealMainPresenter @Inject constructor(
	@ApplicationContext private val androidContext: Context,
	private val androidCoroutineDispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection,
	private val artworkProvider: ArtworkProvider,
	private val sharedRepository: MediaConnectionRepository,
	private val mediaStore: MediaStoreProvider,
) : MainPresenter {

	private val _lock = Any()

	/* @Volatile */
	@GuardedBy("lock")
	private var _actual: Actual? = null

	override val intentHandler: IntentHandler
		get() {
			return _actual?.intentHandler
				?: uninitializedError {
					"The Class is `Not` for late initialization, but for convenience of dependency injection." +
						"It should be made visible after `initialize` returns, you should reconsider your design"
				}
		}

	override fun initialize(
		viewModel: MainPresenter.ViewModel,
		coroutineContext: CoroutineContext
	) {
		sync(_lock) {
			if (_actual != null) {
				alreadyInitializedError()
			}
			_actual = Actual(
				viewModel = viewModel,
				coroutineContext = coroutineContext,
				coroutineDispatchers = androidCoroutineDispatchers,
				androidContext = androidContext,
				playbackConnection = playbackConnection,
				artworkProvider,
				sharedRepository,
				mediaStore
			)
		}
	}

	override fun dispose() {
		sync(_lock) {
			_actual?.dispose()
		}
	}

	private fun uninitializedError(lazyMsg: () -> Any? = {}): Nothing =
		error("MainPresenter was not Initialized, msg: ${lazyMsg().toString()}")
	private fun alreadyInitializedError(lazyMsg: () -> Any? = {}): Nothing =
		error("MainPresenter was already Initialized, msg: ${lazyMsg().toString()}")

	@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
	private class Actual(
		private val viewModel: MainPresenter.ViewModel,
		private val coroutineContext: CoroutineContext,
		override val coroutineDispatchers: AndroidCoroutineDispatchers,
		override val androidContext: Context,
		override val playbackConnection: PlaybackConnection,
		override val artworkProvider: ArtworkProvider,
		override val sharedRepository: MediaConnectionRepository,
		override val mediaStore: MediaStoreProvider,
	) : MainPresenter, MediaIntentHandler.Presenter {

		private val _coroutineScope: CoroutineScope
		private val _mediaIntentHandler: MediaIntentHandler
		private var _disposed = false

		init {
			val sJob = SupervisorJob(coroutineContext[Job])
			val sDispatcher =
				runCatching {
					coroutineContext[CoroutineDispatcher]?.limitedParallelism(1)
				}.getOrNull()
					?: NonBlockingDispatcherPool.get(1)
			_coroutineScope = CoroutineScope(context = sJob + sDispatcher)

			_mediaIntentHandler = MediaIntentHandler(this)

			// load saver from [viewModel] here
		}

		override val intentHandler: IntentHandler = RealIntentHandler(_mediaIntentHandler)

		override fun initialize(
			viewModel: MainPresenter.ViewModel,
			coroutineContext: CoroutineContext
		) = error("Initialized by constructor")

		@GuardedBy("_lock")
		override fun dispose() {
			// backed by outer _lock
			if (_disposed) return
			_disposed = true
			intentHandler.dispose()
		}

		override val coroutineScope: CoroutineScope
			get() = _coroutineScope

		override fun showIntentRequestErrorMessage(message: String) {
			viewModel.showIntentRequestErrorMessage(message)
		}
	}
}

