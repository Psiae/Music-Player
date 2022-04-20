package com.kylentt.mediaplayer.app.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppSettings(
  val navigationSettings: NavigationSettings,
  val wallpaperSettings: WallpaperSettings,
  val isValid: Boolean = true
) {

  companion object {
    val DEFAULT = AppSettings(
      navigationSettings = NavigationSettings.DEFAULT,
      wallpaperSettings = WallpaperSettings.DEFAULT
    )
    val INVALID = DEFAULT.copy(isValid = false)
  }

}

object AppSettingsSerializer : Serializer<AppSettings> {
  override val defaultValue: AppSettings
    get() = AppSettings.DEFAULT

  @OptIn(InternalSerializationApi::class)
  override suspend fun readFrom(input: InputStream): AppSettings {
    return try {
      Json.decodeFromString(
        deserializer = AppSettings::class.serializer(),
        string = input.readBytes().decodeToString()
      )
    } catch (e: Exception) {
      Timber.e(e)
      defaultValue
    }
  }

  @OptIn(InternalSerializationApi::class)
  @Suppress("BlockingMethodInNonBlockingContext")
  override suspend fun writeTo(t: AppSettings, output: OutputStream) {
    try {
      output.write(
        Json.encodeToString(
          serializer = AppSettings::class.serializer(),
          value = t
        ).encodeToByteArray()
      )
    } catch (e: Exception) {
      Timber.e(e)
    }
  }
}
