package com.flammky.musicplayer.base.auth

import kotlinx.collections.immutable.persistentMapOf

object LocalAuth {
	const val ProviderID = "LOCAL"

	fun buildAuthData(): AuthData = AuthData(persistentMapOf(KEY to SIGN))

	internal object KEY : AuthSerializable()
	internal object SIGN : AuthSerializable()
}
