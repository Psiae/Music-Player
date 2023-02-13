package com.flammky.musicplayer.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.LocalAuth
import com.flammky.musicplayer.base.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
internal class AuthGuardViewModel @Inject constructor(
	private val auth: AuthService
) : ViewModel() {

	val currentUser
		get() = auth.currentUser

	val currentUserFlow
		get() = auth.observeCurrentUser()

	fun loginRememberAsync(): Deferred<User?> = viewModelScope.async {
		auth.initialize().join()
		(auth.state as? AuthService.AuthState.LoggedIn)?.user
	}

	fun loginLocalAsync(): Deferred<User?> = viewModelScope.async {
		val data = LocalAuth.buildAuthData()
		when (val result = auth.loginAsync(LocalAuth.ProviderID, data).await()) {
			is AuthService.LoginResult.Success -> result.user
			is AuthService.LoginResult.Error -> error("LoginLocal was failed, ex=${result.ex}")
		}
	}
}
