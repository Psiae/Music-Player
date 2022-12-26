package com.flammky.musicplayer.base.auth

import kotlinx.collections.immutable.ImmutableMap

class AuthData internal constructor(
	private val bundles: ImmutableMap<Any, Any>
) {
	@Suppress("UNCHECKED_CAST")
	operator fun <K, E> get(key: K): E? where K: Any, E: Any = bundles[key] as? E
}
