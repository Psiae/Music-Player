package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.ui.Theme
import com.flammky.musicplayer.ui.common.compose.CircularProgressIndicator

// TODO
@Composable
internal fun authGuard(
	allowShowContentState: State<Boolean>
): State<Boolean?> {
	val vm: MainViewModel = viewModel()
	val allow = remember { mutableStateOf<Boolean?>(vm.currentUser != null) }
	val showLoading = remember { mutableStateOf(vm.currentUser == null) }

	LaunchedEffect(
		key1 = null,
		block = {
			if (vm.currentUser == null) {
				if (vm.rememberAuthAsync().await() == null) {
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
			val intentHandler = vm.intentHandler
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
			modifier = Modifier
				.fillMaxSize()
				.background(Theme.dayNightAbsoluteColor().copy(alpha = 0.94f))
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


	return allow
}
