package com.flammky.musicplayer.base.auth.r

import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.r.user.AuthUser

internal abstract class AuthProvider(
	val id: String
) {
	abstract suspend fun login(data: AuthData): AuthUser
	abstract suspend fun logout(user: AuthUser)
}
