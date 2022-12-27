package com.flammky.musicplayer.base.auth

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@kotlinx.serialization.Serializable
class AuthData internal constructor(
	private val bundles: ImmutableMap<out AuthSerializable, out AuthSerializable>
) {
	@Suppress("UNCHECKED_CAST")
	operator fun <K> get(key: K): AuthSerializable? where K: AuthSerializable = bundles[key]

	companion object {
		val UNSET = AuthData(persistentMapOf())
	}
}

@kotlinx.serialization.Serializable
abstract class AuthSerializable
