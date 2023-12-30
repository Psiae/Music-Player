package com.flammky.musicplayer.base.auth.r

import com.flammky.musicplayer.base.auth.AuthData
import com.flammky.musicplayer.base.auth.AuthSerializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@kotlinx.serialization.Serializable
public class AuthEntity(
	val authData: AuthData,
	val authProviderID: String
) {

	companion object {
		val UNSET = AuthEntity(
			AuthData.UNSET,
			"UNSET"
		)
	}
}
