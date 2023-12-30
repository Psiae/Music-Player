@file:OptIn(ExperimentalSerializationApi::class)

package com.flammky.musicplayer.base.auth

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule

object LocalAuth {
	const val ProviderID = "LOCAL"
	fun buildAuthData(): AuthData = AuthData(AuthData.Map(persistentMapOf(KEY to SIGN)))

	@kotlinx.serialization.Serializable
	internal object KEY : AuthSerializable("com.flammky.musicplayer.base.auth.KEY")

	@kotlinx.serialization.Serializable
	internal object SIGN : AuthSerializable("com.flammky.musicplayer.base.auth.SIGN")

	private val authSerializableModule = SerializersModule {
		polymorphic(AuthSerializable::class, KEY::class, KEY.serializer())
		polymorphic(AuthSerializable::class, SIGN::class, SIGN.serializer())
	}

	fun getAuthSerializableModule(): SerializersModule  {
		return authSerializableModule
	}
}
