package com.flammky.musicplayer.root

import androidx.lifecycle.ViewModel

internal class RuntimePermissionGuardViewModel() : ViewModel() {
	private val saverMap = mutableMapOf<Any, Any>()

	fun save(key: Any, obj: Any) {
		saverMap[key] = obj
	}

	fun getSaved(key: Any): Any? {
		return saverMap[key]
	}

	fun removeSaved(key: Any): Any? {
		return saverMap.remove(key)
	}

	override fun onCleared() {
		saverMap.clear()
	}
}
