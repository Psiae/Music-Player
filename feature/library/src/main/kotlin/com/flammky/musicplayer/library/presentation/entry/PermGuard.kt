package com.flammky.musicplayer.library.presentation.entry

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Composable
internal fun PermGuard(
	onPermChanged: (Boolean?) -> Unit
) {
	val contextHelper = rememberLocalContextHelper()
	val vm = hiltViewModel<PermGuardViewModel>()

	val allowState = remember {
		mutableStateOf<Boolean?>(contextHelper.permissions.common.hasReadExternalStorage)
	}

	val lo = LocalLifecycleOwner.current
	DisposableEffect(key1 = lo, effect = {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				allowState.value = contextHelper.permissions.common.hasReadExternalStorage
			}
		}
		lo.lifecycle.addObserver(observer)
		onDispose { lo.lifecycle.removeObserver(observer) }
	})

	val allow = allowState.value
	LaunchedEffect(key1 = allow, block = {
		onPermChanged(allow)
	})
}


@HiltViewModel
private class PermGuardViewModel @Inject constructor() : ViewModel() {

}
