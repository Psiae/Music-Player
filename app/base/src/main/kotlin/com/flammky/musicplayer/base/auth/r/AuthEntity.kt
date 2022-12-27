package com.flammky.musicplayer.base.auth.r

import androidx.datastore.core.Serializer
import com.flammky.musicplayer.base.auth.AuthData
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@kotlinx.serialization.Serializable
internal class AuthEntity(
	val authData: AuthData,
	val authProviderID: String
) {

	companion object {
		val UNSET = AuthEntity(
			AuthData.UNSET,
			"UNSET"
		)
	}

	object DataStoreSerializer : Serializer<AuthEntity> {

		override val defaultValue: AuthEntity = UNSET

		override suspend fun readFrom(input: InputStream): AuthEntity {
			// TODO: Decrypt
			return Json.decodeFromString(AuthEntity.serializer(), input.readBytes().decodeToString())
		}

		override suspend fun writeTo(t: AuthEntity, output: OutputStream) {
			// TODO: Encrypt
			output.write(Json.encodeToString(AuthEntity.serializer(), t).encodeToByteArray())
		}
	}
}
