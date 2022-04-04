package com.kylentt.musicplayer.ui.preferences

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppState(
    val navigationIndex: Int = 0
) {

    object Defaults {
        const val invalidNavigationIndex = -11
        val INVALID = AppState(navigationIndex = invalidNavigationIndex)
    }


}


object AppStateSerializer : Serializer<AppState> {
    override val defaultValue: AppState
        get() = AppState()

    override suspend fun readFrom(input: InputStream): AppState {
        return try {
            Json.decodeFromString(
                deserializer = AppState.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e : Exception) {
            Timber.e(e)
            defaultValue
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AppState.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}