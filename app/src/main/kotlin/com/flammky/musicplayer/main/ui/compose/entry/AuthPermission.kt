package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.foundation.background
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
internal fun authPermissionEntryState(): State<Boolean?> {
	val vm: MainViewModel = viewModel()
	val state = remember { mutableStateOf<Boolean?>(null) }
	val showLoading = remember { mutableStateOf(false) }

	LaunchedEffect(
		key1 = null,
		block = {
			if (vm.currentUser == null) {
				showLoading.value = true
				if (vm.rememberAuthAsync().await() == null) {
					vm.loginLocalAsync().await()
				}
				showLoading.value = false
			}
			vm.currentUserFlow.collect {
				state.value = it != null
			}
		}
	)

	if (showLoading.value) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Theme.dayNightAbsoluteColor().copy(alpha = 0.94f))
		) {
			CircularProgressIndicator(
				modifier = Modifier.align(Alignment.Center),
				color = MaterialTheme.colorScheme.primary,
				strokeWidth = 3.dp
			)
		}
	}


	return state
}
