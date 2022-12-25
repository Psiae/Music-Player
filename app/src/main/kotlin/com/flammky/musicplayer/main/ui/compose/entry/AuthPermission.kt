package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// TODO
@Composable
internal fun authPermissionEntryState(): State<Boolean> {
	val state = remember { mutableStateOf(true) }

	return state
}
