package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.main.ui.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

// should we do scoping ?
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun permGuard(
	allowShowContentState: State<Boolean>
): State<Boolean?> {
	val contextHelper = rememberLocalContextHelper()
	val mainVM: MainViewModel = viewModel()
	val vm: AndroidPermissionViewModel = viewModel()

	val permissionGrantedState = remember {
		mutableStateOf(contextHelper.permissions.common.hasReadExternalStorage ||
			contextHelper.permissions.common.hasWriteExternalStorage
		)
	}

	val allow = remember {
		mutableStateOf(!vm.persistPager && permissionGrantedState.value)
	}

	val showPagerState = remember {
		mutableStateOf(!allow.value)
	}

	if (!allow.value) {
		val interceptor = remember {
			val intentHandler = mainVM.intentHandler
			intentHandler.createInterceptor()
				.apply {
					setFilter { target ->
						intentHandler.intentRequireAndroidPermission(
							intent = target.cloneActual(),
							permission = AndroidPermission.Read_External_Storage
						)
					}
					start()
				}
		}
		DisposableEffect(
			// wait to be removed from composition tree
			key1 = null
		) {
			onDispose {
				if (!allow.value) {
					// should we tho ?
					interceptor.dropAllInterceptedIntent()
				}
				interceptor.dispose()
			}
		}
	}

	if (!allow.value && allowShowContentState.value) {
		Box(modifier = Modifier.fillMaxSize()) {
			EntryPermissionPager(
				contextHelper = contextHelper,
				onGranted = { allow.value = true ; showPagerState.value = false }
			)
		}
	}

	return allow
}

internal class AndroidPermissionViewModel() : ViewModel() {
	var persistPager = false
}

@Composable
private fun EntryPermissionPager(
	showState: State<Boolean>,
	onEntryAllowed: () -> Unit
) {
	val contextHelper = rememberLocalContextHelper()
	val vm: AndroidPermissionViewModel = viewModel()
}


