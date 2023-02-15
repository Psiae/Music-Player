package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.main.ext.IntentReceiver
import com.flammky.musicplayer.main.ui.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

// should we do scoping ?
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun PermGuard(
	mainVM: MainViewModel,
	entryVM: EntryGuardViewModel,
	allowShowContentState: State<Boolean>
) {
	val contextHelper = rememberLocalContextHelper()
	val vm: AndroidPermissionViewModel = viewModel()

	val permissionGrantedState = remember {
		mutableStateOf(contextHelper.permissions.common.hasReadExternalStorage ||
			contextHelper.permissions.common.hasWriteExternalStorage
		)
	}

	val allow = remember {
		mutableStateOf(!vm.persistPager && permissionGrantedState.value)
	}

	if (!allow.value) {
		vm.persistPager = true
		if (vm.intentInterceptor == null) {
			val intentHandler = mainVM.intentHandler
			vm.intentInterceptor = intentHandler.createInterceptor()
				.apply {
					setFilter { target ->
						intentHandler.intentRequireAndroidPermission(
							intent = target.cloneActual(),
							permission = AndroidPermission.Read_External_Storage
						)
					}
					start()
				}
				.also {
					vm.intentInterceptor = it
				}
		}
		DisposableEffect(
			// wait to be removed from composition tree
			key1 = null
		) {
			onDispose {
				if (allow.value) {
					vm.intentInterceptor!!
						.apply {
							dispatchAllInterceptedIntent()
							dispose()
						}
					vm.persistPager = false
				} else {
					// Probably config change
				}
			}
		}
	}

	if (!allow.value && allowShowContentState.value) {
		Box(modifier = Modifier
			.fillMaxSize()
			.background(Theme.backgroundColorAsState().value)) {
			EntryPermissionPager(
				contextHelper = contextHelper,
				onGranted = { allow.value = true }
			)
		}
	}

	NoInline {
		mainVM.permGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	LaunchedEffect(key1 = allow.value, block = {
		entryVM.permGuardAllow.value = allow.value
	})
}

private inline fun invoke(block: () -> Unit) = block.invoke()

internal class AndroidPermissionViewModel() : ViewModel() {
	var persistPager = false
	var intentInterceptor: IntentReceiver.Interceptor? = null

	override fun onCleared() {
		intentInterceptor
			?.apply {
				dropAllInterceptedIntent()
				dispose()
			}
	}
}

@Composable
private fun EntryPermissionPager(
	showState: State<Boolean>,
	onEntryAllowed: () -> Unit
) {
	val contextHelper = rememberLocalContextHelper()
	val vm: AndroidPermissionViewModel = viewModel()
}


