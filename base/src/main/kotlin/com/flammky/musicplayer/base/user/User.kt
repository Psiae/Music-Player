package com.flammky.musicplayer.base.user

/**
 * Abstract class that represent a USER instance
 *
 * @param uid the User ID
 * @param verify the User verifier
 */
@kotlinx.serialization.Serializable
abstract class User(
	val uid: String,
	val verify: String
) {
	init {
		require(uid.isNotEmpty()) {
			"User ID=$uid cannot be empty"
		}
	}
}
