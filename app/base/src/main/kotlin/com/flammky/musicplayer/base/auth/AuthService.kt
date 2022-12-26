package com.flammky.musicplayer.base.auth

import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.base.user.User
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Global Authentication Service Interface
 */
interface AuthService {

	val currentUser: User?

	/**
	 * Attempt to Log-in asynchronously
	 *
	 * ** Must call [logout] before logging in **
	 *
	 * **
	 * If multiple `login` attempt is made, then it will cancel any ongoing login task including the
	 * job given in [coroutineContext]
	 * **
	 *
	 * @param provider the `ID` of the auth provider
	 * @param data the `Authentication Data` to authenticate
	 * @param coroutineContext optional coroutineContext of the request
	 *
	 * @return deferred of [LoginResult]
	 */
	suspend fun loginAsync(
		provider: String,
		data: AuthData,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<LoginResult>

	fun logout()

	sealed interface LoginResult {
		data class Success(val user: User) : LoginResult
		data class Error(val msg: String, val ex: Exception) : LoginResult
	}

	fun observeCurrentUser(): Flow<User?>

	companion object {
		private val SINGLETON = LazyConstructor<AuthService>()

		infix fun provides(instance: AuthService) = SINGLETON.constructOrThrow(
			lazyValue = { instance },
			lazyThrow = { error("AuthService must only be provided once") }
		)

		fun get(): AuthService = SINGLETON.valueOrNull()
			?: error("AuthService was not provided")
	}
}
