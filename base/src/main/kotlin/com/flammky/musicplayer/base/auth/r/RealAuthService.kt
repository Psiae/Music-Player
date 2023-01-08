package com.flammky.musicplayer.base.auth.r

import android.app.Application
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.dataStore
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.ProviderNotFoundException
import com.flammky.musicplayer.base.auth.r.user.AuthUser
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

internal class RealAuthService(
	private val androidContext: Context,
	private val coroutineDispatchers: AndroidCoroutineDispatchers = AndroidCoroutineDispatchers.DEFAULT
) : AuthService {
	private val _userModification = atomic(0)

	private val eventDispatcher = NonBlockingDispatcherPool.get(1)
	private val ioDispatcher = coroutineDispatchers.io
	private val currentUserChannel = MutableSharedFlow<User?>(1)

	private val Context.authDataStore by dataStore(
		fileName = "auth.r.AuthService",
		serializer = AuthEntity.DataStoreSerializer
	)

	private val _supervisorScope = CoroutineScope(SupervisorJob())
	private val _stateLock = ReentrantReadWriteLock()

	@GuardedBy("_stateLock")
	private var _currentUser: User? = null
		set(value) {
			require(_stateLock.isWriteLockedByCurrentThread) {
				"RealAuthService_Guard: _currentUser was modified without holding `_stateLock`"
			}
			field = value
		}

	@GuardedBy("stateLock")
	private var _currentState: AuthService.AuthState = AuthService.AuthState.Uninitialized

	@GuardedBy("_stateLock")
	private val _providers = mutableMapOf<String, AuthProvider>()

	@GuardedBy("_stateLock")
	private var _loginJob: Job = Job().apply { cancel() }
		set(value) {
			require(_stateLock.isWriteLockedByCurrentThread) {
				"RealAuthService_Guard: _loginJob was modified without holding `_stateLock`"
			}
			field = value
		}

	private val initializeJob: Job = _supervisorScope.launch(
		context = ioDispatcher,
		start = CoroutineStart.LAZY
	) {
		val (deferred: Deferred<User?>, mod: Int) = _stateLock.write {
			if (_loginJob.isActive) {
				error("LoginJob was active")
			}
			if (_currentUser != null) {
				error("CurrentUser was not null")
			}
			val mod = _userModification.incrementAndGet()
			loginRememberedAsync() to mod
		}
		val user = deferred.await()
		withContext(eventDispatcher) {
			if (_userModification.get() == mod) currentUserChannel.emit(user)
		}
	}

	override val currentUser: User?
		get() = _stateLock.read { _currentUser }

	override val state: AuthService.AuthState
		get() = _stateLock.read { _currentState }

	override fun initialize(): Job = initializeJob.apply { start() }

	override suspend fun loginAsync(
		provider: String,
		data: AuthData,
		coroutineContext: CoroutineContext
	): Deferred<AuthService.LoginResult> {
		return _stateLock.write {
			logout()
			_supervisorScope.async(coroutineDispatchers.io + coroutineContext) {
				doLogin(provider, data)
			}.also {
				_loginJob = it
			}
		}
	}

	override fun logout() {
		val (user: AuthUser, mod: Int) = _stateLock.write {
			_loginJob.cancel()
			val currentUser = _currentUser
				?: return
			_currentUser = null
			_currentState = AuthService.AuthState.LoggedOut
			currentUser as AuthUser to _userModification.incrementAndGet()
		}
		sendUserChangeEvent(null, mod)
		_supervisorScope.launch(ioDispatcher) {
			_providers[user.provider]?.logout(user)
		}
	}

	override fun observeCurrentUser(): Flow<User?> {
		return flow {
			initialize().join()
			emitAll(currentUserChannel)
		}
	}

	private suspend fun loginRememberedAsync(): Deferred<User?> {
		return _stateLock.write {
			if (_loginJob.isActive) {
				error("Tried to login remembered during logging-in")
			}
			if (_loginJob.isActive || _currentUser != null) {
				error("Tried to login remembered when already logged-in")
			}
			_supervisorScope.async(ioDispatcher) {
				val dts = androidContext.authDataStore
				val entity = dts.data.first()
				val data = entity.authData
				when (val result = loginAsync(entity.authProviderID, data).await()) {
					is AuthService.LoginResult.Success -> result.user
					else -> null
				}.also {
					Timber.d("LoginRememberedAsync completed: $it")
				}
			}.also {
				_loginJob = it
			}
		}
	}

	private suspend fun doLogin(providerId: String, data: AuthData): AuthService.LoginResult {
		val provider = _providers[providerId]
			?: return AuthService.LoginResult.Error(
				msg = "providerId=$providerId was not found",
				ex = ProviderNotFoundException("providerId=$providerId was not found")
			)
		val user = runCatching {
			provider.login(data)
				.apply {
					androidContext.authDataStore.updateData { writeEntity() }
				}
		}.getOrElse { throwable ->
			return AuthService.LoginResult.Error(
				msg = (throwable as Exception).message ?: "",
				ex = throwable
			)
		}
		val mod = _stateLock.write {
			if (coroutineContext.isActive) {
				_currentUser = user
				_currentState = AuthService.AuthState.LoggedIn(user)
				_userModification.incrementAndGet()
			} else {
				_supervisorScope.launch(ioDispatcher) {
					_stateLock.read {
						_providers[user.provider]?.logout(user)
					}
				}
				return AuthService.LoginResult.Error(
					msg = "Login cancelled",
					ex = CancellationException("Login cancelled")
				)
			}
		}
		sendUserChangeEvent(user, mod)
		return AuthService.LoginResult.Success(user = user)
	}

	// I think we should `trysend` within the lock as the channel is conflated anyways
	private fun sendUserChangeEvent(
		user: User?, mod: Int
	): Job {
		return _supervisorScope.launch(eventDispatcher) {
			if (_userModification.get() == mod) currentUserChannel.emit(user)
		}
	}

	private fun registerProvider(provider: AuthProvider) {
		_stateLock.write {
			_providers.putIfAbsent(provider.id, provider)
		}
	}

	companion object {
		private val INSTANCE = LazyConstructor<RealAuthService>()
		internal infix fun provides(app: Application) = INSTANCE.constructOrThrow(
			lazyValue = { RealAuthService(app) },
			lazyThrow = { error("RealAuthService was already provided") }
		)
		fun registerProvider(provider: AuthProvider) = INSTANCE.valueOrNull()
			?.apply { registerProvider(provider) }
			?: error("RealAuthService was not provided")
	}
}
