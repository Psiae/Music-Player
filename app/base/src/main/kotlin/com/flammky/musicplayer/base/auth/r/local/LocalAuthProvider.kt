package com.flammky.musicplayer.base.auth.r.local

import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.r.AuthProvider
import com.flammky.musicplayer.base.auth.r.user.AuthUser

internal class LocalAuthProvider : AuthProvider(ID) {

	override suspend fun login(data: AuthData): AuthUser {
		TODO("Not yet implemented")
	}

	override suspend fun logout(user: AuthUser) {
		TODO("Not yet implemented")
	}

	companion object {
		const val ID = "LOCAL"
	}
}
