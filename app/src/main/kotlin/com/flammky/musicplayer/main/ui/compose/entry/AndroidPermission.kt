package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi

// should we do scoping ?
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun androidPermissionEntryState(): State<Boolean?> {
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

	val showPagerState = remember {
		mutableStateOf(!allow.value)
	}

	if (!allow.value) {
		com.flammky.musicplayer.ui.main.compose.entry.EntryPermissionPager(
			contextHelper = contextHelper,
			onGranted = { allow.value = true ; showPagerState.value = false }
		)
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


