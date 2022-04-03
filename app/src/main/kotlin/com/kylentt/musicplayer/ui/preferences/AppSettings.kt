package com.kylentt.musicplayer.ui.preferences

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppSettings(
    val wallpaperSettings: WallpaperSettings = WallpaperSettings.Defaults.getDefault()
) {

    @Serializable
    object Defaults {
        val invalid = AppSettings(wallpaperSettings = WallpaperSettings.Defaults.Invalid)
    }

}


@Serializable
data class WallpaperSettings(
    val mode: Mode,
    val source: Source,
    val sourceAlternative: SourceAlternative,
    val isValid: Boolean = true
) {

    @Serializable
    object Defaults {
        fun getDefault() = WallpaperSettings(Mode.NAVIGATION, Source.DEFAULT, SourceAlternative.DEVICE_WALLPAPER)
        val Invalid = getDefault().copy(isValid = false)
    }

    enum class Mode {
        NORMAL,
        NAVIGATION,
    }

    enum class Source {
        DEFAULT,
        DEVICE_WALLPAPER,
        MEDIA_ITEM
    }

    enum class SourceAlternative {
        DEFAULT,
        DEVICE_WALLPAPER,
        MEDIA_ITEM
    }
}

object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            Json.decodeFromString(
                deserializer = AppSettings.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e : Exception) {
            Timber.e(e)
            defaultValue
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AppSettings.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}