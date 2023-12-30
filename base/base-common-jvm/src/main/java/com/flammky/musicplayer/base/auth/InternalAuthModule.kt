package com.flammky.musicplayer.base.auth

import kotlinx.serialization.modules.SerializersModule

internal object InternalAuthModule {

	fun getAuthObjectSerializerModule(): List<SerializersModule> {
		return listOf(
			LocalAuth.getAuthSerializableModule(),
			FirebaseAuth.getAuthSerializableModule(),
			GoogleAuth.getAuthSerializableModule(),
		)
	}
}
