package com.flammky.musicplayer.root

import androidx.lifecycle.ViewModel

internal class RuntimePermissionGuardViewModel() : ViewModel() {
	private val saverMap = mutableMapOf<String, Any>()
}
