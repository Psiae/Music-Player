package com.flammky.musicplayer.main.ui.r

import android.content.Context
import androidx.annotation.GuardedBy
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.LocalAuth
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.main.ext.IntentReceiver
import com.flammky.musicplayer.main.ext.MediaIntentHandler
import com.flammky.musicplayer.main.ui.MainPresenter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ExpectMainPresenter @Inject constructor(
	private val authService: AuthService,
	@ApplicationContext private val androidContext: Context,
	private val androidCoroutineDispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection,
	private val artworkProvider: ArtworkProvider,
	private val sharedRepository: MediaMetadataCacheRepository,
	private val mediaStore: MediaStoreProvider,
) : MainPresenter {

	private val _lock = Any()

	/* @Volatile */
	@GuardedBy("lock")
	private var _actual: Actual? = null

	override val intentReceiver: IntentReceiver
		get() {
			return _actual?.intentReceiver
				?: uninitializedError {
					"The Class is `Not` for late initialization, but for convenience of dependency injection." +
						"It should be made visible after `initialize` returns, you should reconsider your design"
				}
		}

	override val auth: MainPresenter.Auth
		get() {
			return _actual?.auth
				?: uninitializedError {
					"The Class is `Not` for late initialization, but for convenience of dependency injection." +
						"It should be made visible after `initialize` returns, you should reconsider your design"
				}
		}

	/**
	 * Initialize the instance
	 *
	 * @param viewModel the ViewModel
	 */
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
				authService = authService,
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
		override val authService: AuthService,
		override val coroutineDispatchers: AndroidCoroutineDispatchers,
		override val androidContext: Context,
		override val playbackConnection: PlaybackConnection,
		override val artworkProvider: ArtworkProvider,
		override val sharedRepository: MediaMetadataCacheRepository,
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

		override val intentReceiver: IntentReceiver = RealIntentReceiver(_mediaIntentHandler)
		override val auth: MainPresenter.Auth = object : MainPresenter.Auth {
			override val currentUserFlow: Flow<User?>
				get() = authService.observeCurrentUser()
			override val currentUser: User?
				get() = authService.currentUser

			override fun rememberAuthAsync(coroutineContext: CoroutineContext): Deferred<User?> {
				return _coroutineScope.async(coroutineContext) {
					authService.initialize().join()
					(authService.state as? AuthService.AuthState.LoggedIn)?.user
				}
			}

			override fun loginRememberedAsync(coroutineContext: CoroutineContext): Deferred<User?> {
				return TODO()
			}

			override fun loginLocalAsync(coroutineContext: CoroutineContext): Deferred<User> {
				return _coroutineScope.async(coroutineContext) {
					val data = LocalAuth.buildAuthData()
					when (val result = authService.loginAsync(LocalAuth.ProviderID, data).await()) {
						is AuthService.LoginResult.Success -> result.user
						is AuthService.LoginResult.Error -> error("LoginLocal was failed, ex=${result.ex}")
					}.also {
						Timber.d("LoginLocalAsync completed: $it")
					}
				}
			}
		}

		override fun initialize(
			viewModel: MainPresenter.ViewModel,
			coroutineContext: CoroutineContext
		) = error("Initialized by constructor")

		@GuardedBy("_lock")
		override fun dispose() {
			// backed by outer _lock
			if (_disposed) return
			_disposed = true
			intentReceiver.dispose()
		}

		override val coroutineScope: CoroutineScope
			get() = _coroutineScope

		override fun showIntentRequestErrorMessage(message: String) {
			viewModel.showIntentRequestErrorMessage(message)
		}
	}
}

