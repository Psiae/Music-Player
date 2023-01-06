package com.flammky.musicplayer.base.auth.r.user

import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.r.AuthEntity
import com.flammky.musicplayer.base.user.User
import kotlinx.collections.immutable.persistentMapOf

internal abstract class AuthUser(
	uid: String,
	val provider: String
) : User(uid, provider) {

	abstract fun writeEntity(): AuthEntity

	object UNSET : AuthUser("UNSET_UNSET", "UNSET") {
		override fun writeEntity(): AuthEntity = AuthEntity(
			authData = AuthData(AuthData.Map(persistentMapOf())),
			authProviderID =  "UNSET"
		)
	}
}
