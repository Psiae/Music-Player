package com.flammky.musicplayer.base.auth

import kotlinx.serialization.modules.SerializersModule

object FirebaseAuth {

	fun getAuthSerializableModule(): SerializersModule = SerializersModule {  }
}
