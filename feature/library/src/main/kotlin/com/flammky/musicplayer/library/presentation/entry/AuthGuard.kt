package com.flammky.musicplayer.library.presentation.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Composable
internal fun AuthGuard(
	onAuthChanged: (Boolean?) -> Unit
) {
	val vm = hiltViewModel<AuthGuardViewModel>()
	val authState = remember {
		mutableStateOf<Boolean?>(null)
	}

	LaunchedEffect(key1 = Unit, block = {
		vm.observeCurrentUser().collect {
			authState.value = it != null
		}
	})

	val state = authState.value
	LaunchedEffect(key1 = state, block = {
		onAuthChanged(state)
	})
}


@HiltViewModel
private class AuthGuardViewModel @Inject constructor(
	private val authService: AuthService
) : ViewModel() {

	fun observeCurrentUser(): Flow<User?> = authService.observeCurrentUser()
}
