package com.flammky.musicplayer.main.ui.compose.entry

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class EntryGuardViewModel() : ViewModel() {
	val authGuardAllow = MutableStateFlow<Boolean?>(null)
	val permGuardAllow = MutableStateFlow<Boolean?>(null)
}
