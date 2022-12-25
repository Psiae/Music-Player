package com.flammky.musicplayer.main.ui.r

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.main.ext.IntentHandler
import com.flammky.musicplayer.main.ui.MainPresenter
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class RealMainPresenter : MainPresenter {

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
			_actual = Actual(viewModel, coroutineContext)
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
		viewModel: MainPresenter.ViewModel,
		coroutineContext: CoroutineContext
	) : MainPresenter {

		private val _coroutineScope: CoroutineScope
		private var _disposed = false

		init {
			val sJob = SupervisorJob(coroutineContext[Job])
			val sDispatcher =
				runCatching {
					coroutineContext[CoroutineDispatcher]?.limitedParallelism(1)
				}.getOrNull()
					?: NonBlockingDispatcherPool.get(1)
			_coroutineScope = CoroutineScope(context = sJob + sDispatcher)

			// load saver from [viewModel] here
		}

		override val intentHandler: IntentHandler = RealIntentHandler()

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
	}
}

