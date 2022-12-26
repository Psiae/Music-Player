package com.flammky.musicplayer.base.auth.r.user

import com.flammky.musicplayer.base.user.User

internal abstract class AuthUser(
	uid: String,
	val provider: String
) : User(uid) {

}
