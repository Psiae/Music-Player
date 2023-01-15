package com.flammky.musicplayer.library.presentation.entry

import androidx.compose.runtime.*

@Composable
internal fun EntryGuard(content: @Composable () -> Unit) {
	if (guard().value == true) {
		content()
	}
}

@Composable
private fun guard(): State<Boolean?> {
	val authState = remember {
		mutableStateOf<Boolean?>(null)
	}
	val permState = remember {
		mutableStateOf<Boolean?>(null)
	}

	AuthGuard() {
		authState.value = it
	}

	PermGuard() {
		permState.value = it
	}

	return remember {
		derivedStateOf {
			authState.value?.let { auth ->
				permState.value?.let { perm ->
					auth && perm
				}
			}
		}
	}
}
