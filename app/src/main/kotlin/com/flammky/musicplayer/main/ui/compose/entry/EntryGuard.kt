package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.main.ui.MainViewModel

@Composable
internal fun EntryGuard(
	content: @Composable () -> Unit
) {
	if (contentAllowState().value) {
		content()
	}
}

@Composable
private fun contentAllowState(): State<Boolean> {
	val state = androidPermissionEntryState()
	val vm: MainViewModel = viewModel()

	if (state.value) {
		return state
	}

	val interceptor = remember {
		val intentHandler = vm.intentHandler
		intentHandler.createInterceptor()
			.apply {
				setFilter {
					val clone = it.cloneActual()
					intentHandler.intentRequireAndroidPermission(clone, AndroidPermission.Read_External_Storage)
				}
				start()
			}
	}
	DisposableEffect(
		// wait to be removed from composition tree by either `state.value` become true
		// or parent branch
		key1 = null
	) {
		onDispose {
			if (!state.value) {
				// should we tho ?
				interceptor.dropAllInterceptedIntent()
			}
			interceptor.dispose()
		}
	}

	return state
}
