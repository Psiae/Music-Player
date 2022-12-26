package com.flammky.musicplayer.base.auth.r

import androidx.annotation.GuardedBy
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.kotlin.common.sync.atomic
import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.ProviderNotFoundException
import com.flammky.musicplayer.base.auth.r.user.AuthUser
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.user.User
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

internal class RealAuthService(
	val coroutineDispatchers: AndroidCoroutineDispatchers = AndroidCoroutineDispatchers.DEFAULT
) : AuthService {
	private val _userModification = atomic(0)

	private val eventDispatcher = NonBlockingDispatcherPool.get(1)
	private val ioDispatcher = coroutineDispatchers.io
	private val currentUserChannel = Channel<User?>(Channel.CONFLATED)

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

	@GuardedBy("_stateLock")
	private val _providers = mutableMapOf<String, AuthProvider>()

	@GuardedBy("_stateLock")
	private var _loginJob: Job = Job().apply { cancel() }

	override val currentUser: User?
		get() = _stateLock.read { _currentUser }

	override suspend fun loginAsync(
		provider: String,
		data: AuthData,
		coroutineContext: CoroutineContext
	): Deferred<AuthService.LoginResult> {
		return _stateLock.write {
			_loginJob.cancel()
			_supervisorScope.async(coroutineDispatchers.io + coroutineContext) {
				doLogin(provider, data)
			}.also {
				_loginJob = it
			}
		}
	}

	override fun logout() {
		val (user: AuthUser, mod: Int) = _stateLock.write {
			val currentUser = _currentUser ?: return
			_currentUser = null
			currentUser as AuthUser to _userModification.incrementAndGet()
		}
		sendUserChangeEvent(null, mod)
		_supervisorScope.launch(ioDispatcher) {
			_providers[user.provider]?.logout(user)
		}
	}

	override fun observeCurrentUser(): Flow<User?> {
		return currentUserChannel.receiveAsFlow()
	}

	private suspend fun doLogin(providerId: String, data: AuthData): AuthService.LoginResult {
		val provider = _providers[providerId]
			?: return AuthService.LoginResult.Error(
				msg = "providerId=$providerId was not found",
				ex = ProviderNotFoundException("providerId=$providerId was not found")
			)
		val user = runCatching {
			provider.login(data).also { coroutineContext.ensureActive() }
		}.getOrElse { throwable ->
			return AuthService.LoginResult.Error(
				msg = (throwable as Exception).message ?: "",
				ex = throwable
			)
		}
		val mod = _stateLock.write {
			_currentUser = user
			_userModification.incrementAndGet()
		}
		sendUserChangeEvent(user, mod)
		return AuthService.LoginResult.Success(user = user)
	}

	// I think we should `trysend` within the lock as the channel is conflated anyways
	private fun sendUserChangeEvent(
		user: User?, mod: Int
	): Job {
		return _supervisorScope.launch(eventDispatcher) {
			if (_userModification.get() == mod) currentUserChannel.send(user)
		}
	}

	private fun registerProvider(provider: AuthProvider) {
		_stateLock.write {
			_providers.putIfAbsent(provider.id, provider)
		}
	}

	companion object {
		private val INSTANCE = RealAuthService()
		fun registerProvider(provider: AuthProvider) = INSTANCE.registerProvider(provider)
	}
}
