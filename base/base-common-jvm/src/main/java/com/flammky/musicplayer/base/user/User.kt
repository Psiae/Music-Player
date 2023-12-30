package com.flammky.musicplayer.base.user

import java.util.*

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
		require(verify.isNotEmpty()) {
			"User Verify=$verify cannot be empty"
		}
	}

	override fun equals(other: Any?): Boolean {
		return other is User &&
			other.uid == this.uid &&
			other.verify == this.verify
	}

	override fun hashCode(): Int {
		return Objects.hash(uid, verify)
	}
}
