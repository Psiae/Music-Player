package com.flammky.musicplayer.base.user

abstract class User(
	val uid: String
) {
	init {
		require(uid.isNotEmpty()) {
			"User ID=$uid cannot be empty"
		}
	}
}
