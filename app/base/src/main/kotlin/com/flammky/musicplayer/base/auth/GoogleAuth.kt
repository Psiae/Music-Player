package com.flammky.musicplayer.base.auth

import kotlinx.serialization.modules.SerializersModule

object GoogleAuth {

	fun getAuthSerializableModule(): SerializersModule = SerializersModule {  }
}
