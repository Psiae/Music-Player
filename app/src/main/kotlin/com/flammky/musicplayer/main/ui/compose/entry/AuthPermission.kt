package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.auth.LocalAuth
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundContentColorAsState
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.main.ui.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import timber.log.Timber
import javax.inject.Inject

// TODO
@Composable
internal fun AuthGuard(
	mainVM: MainViewModel,
	entryVM: EntryGuardViewModel,
	allowShowContentState: State<Boolean>,
	modifier: Modifier = Modifier,
) {
	val vm: AuthGuardViewModel = viewModel()
	val allow = remember { mutableStateOf<Boolean?>(vm.currentUser != null) }
	val showLoading = remember { mutableStateOf(false) }

	LaunchedEffect(
		key1 = null,
		block = {
			if (vm.currentUser == null) {
				val remember = vm.loginRememberAsync()
				showLoading.value = true
				if (remember.await() == null) {
					vm.loginLocalAsync().await()
				}
			}
			showLoading.value = false
			vm.currentUserFlow.collect {
				allow.value = it != null
			}
		}
	)

	if (allow.value != true) {
		val interceptor = remember {
			val intentHandler = mainVM.intentHandler
			intentHandler.createInterceptor()
				.apply {
					setFilter { target ->
						intentHandler.intentRequireAuthPermission(target.cloneActual())
					}
					start()
				}
		}
		DisposableEffect(
			// wait to be removed from composition tree
			key1 = null
		) {
			onDispose {
				if (allow.value != true) {
					// should we tho ?
					interceptor.dropAllInterceptedIntent()
				}
				interceptor.dispose()
			}
		}
	}

	if (showLoading.value && allowShowContentState.value) {
		Box(
			modifier = modifier
				.fillMaxSize()
				.background(Theme.absoluteBackgroundContentColorAsState().value.copy(alpha = 0.94f))
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = {}
				)
		) {
			// we should probably allow a loading queue
			CircularProgressIndicator(
				modifier = Modifier.align(Alignment.Center),
				color = MaterialTheme.colorScheme.primary,
				strokeWidth = 3.dp
			)
		}
	}

	NoInline {
		mainVM.authGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	LaunchedEffect(key1 = allow.value, block = {
		entryVM.authGuardAllow.value = allow.value
	})
}

private inline fun invoke(block: () -> Unit) = block.invoke()

@HiltViewModel
private class AuthGuardViewModel @Inject constructor(
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
		}.also {
			Timber.d("LoginLocalAsync completed: $it")
		}
	}
}
