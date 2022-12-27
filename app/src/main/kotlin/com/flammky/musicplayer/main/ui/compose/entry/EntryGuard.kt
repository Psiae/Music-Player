package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.ui.main.compose.entry.NoInline

@Composable
internal fun EntryGuard(
	content: @Composable () -> Unit
) {
	if (contentAllowState().value == true) {
		content()
	}
}

@Composable
private fun contentAllowState(): State<Boolean?> {
	// should switch
	val allowState = remember { mutableStateOf<Boolean?>(null) }
	val authState = authPermissionEntryState()

	if (authState.value != true) {
		return authState
	}

	val permState = androidPermissionEntryState()

	val vm: MainViewModel = viewModel()
	val auth = authState.value
	val perm = permState.value
	allowState.value = if (auth == null || perm == null ) null else auth && perm

	if (perm == false) {
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
			// wait to be removed from composition tree
			key1 = null
		) {
			onDispose {
				if (permState.value != true) {
					// should we tho ?
					interceptor.dropAllInterceptedIntent()
				}
				interceptor.dispose()
			}
		}
	}

	NoInline {
		vm.entryCheckWaiter.forEach { it() }
		vm.entryCheckWaiter.clear()
	}

	return allowState
}
