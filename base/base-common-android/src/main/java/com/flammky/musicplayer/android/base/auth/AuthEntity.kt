package com.flammky.musicplayer.android.base.auth

import androidx.datastore.core.Serializer
import com.flammky.musicplayer.base.auth.AuthSerializable
import com.flammky.musicplayer.base.auth.r.AuthEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@ExperimentalSerializationApi
object DataStoreSerializer : Serializer<AuthEntity> {

	private val structuredJson = Json {
		allowStructuredMapKeys = true
		serializersModule = AuthSerializable.module
	}

	override val defaultValue: AuthEntity = AuthEntity.UNSET

	override suspend fun readFrom(input: InputStream): AuthEntity {
		// TODO: Decrypt
		return structuredJson.decodeFromString(AuthEntity.serializer(), input.readBytes().decodeToString())
	}

	override suspend fun writeTo(t: AuthEntity, output: OutputStream) {
		// TODO: Encrypt
		output.write(structuredJson.encodeToString(AuthEntity.serializer(), t).encodeToByteArray())
	}
}

@ExperimentalSerializationApi
val AuthEntity.Companion.dataStoreSerializer
	get() = DataStoreSerializer
