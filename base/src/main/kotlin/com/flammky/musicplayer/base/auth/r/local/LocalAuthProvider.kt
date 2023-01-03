package com.flammky.musicplayer.base.auth.r.local

import android.app.Application
import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.InvalidAuthDataException
import com.flammky.musicplayer.base.auth.LocalAuth
import com.flammky.musicplayer.base.auth.r.AuthEntity
import com.flammky.musicplayer.base.auth.r.AuthProvider
import com.flammky.musicplayer.base.auth.r.user.AuthUser

internal class LocalAuthProvider(
	private val application: Application
) : AuthProvider(ID) {

	override suspend fun login(data: AuthData): AuthUser {
		require(data[LocalAuth.KEY] == LocalAuth.SIGN) {
			throw InvalidAuthDataException("Missing Signature")
		}
		return LocalUser
	}

	override suspend fun logout(user: AuthUser) = Unit

	private object LocalUser : AuthUser(AUTH_SPACE + "LOCAL", ID) {
		override fun writeEntity(): AuthEntity = AuthEntity(
			authData = LocalAuth.buildAuthData(),
			authProviderID = ID
		)
	}

	companion object {
		private const val AUTH_SPACE = "LOCAL_"
		const val ID = "LOCAL"
	}
}
