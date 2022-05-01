package com.kylentt.mediaplayer.app.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Class to store the Application Settings, @Serializable for [androidx.datastore.core.DataStore]
 * @author Kylentt
 * @since 2022/04/30
 * @constructor [navigationSettings] the Navigation Settings
 * @constructor [wallpaperSettings] the Wallpaper Settings
 * @constructor [isValid] whether the instance is a Valid [AppSettings] or a Placeholder
 * @property [Companion.DEFAULT] Static Default Instance
 * @property [Companion.INVALID] Static Invalid Instance
 * @see [NavigationSettings]
 * @see [WallpaperSettings]
 * @see [AppSettingsSerializer]
 */

@Serializable
data class AppSettings(
  val navigationSettings: NavigationSettings,
  val wallpaperSettings: WallpaperSettings,
  val isValid: Boolean = true
) {

  companion object {
    @JvmStatic val DEFAULT by lazy {
      val navigation = NavigationSettings.DEFAULT
      val wallpaper = WallpaperSettings.DEFAULT
      AppSettings(navigationSettings = navigation, wallpaperSettings = wallpaper)
    }
    @JvmStatic
    val INVALID = DEFAULT.copy(isValid = false)
  }
}

object AppSettingsSerializer : Serializer<AppSettings> {

  override val defaultValue: AppSettings
    get() = AppSettings.DEFAULT

  @OptIn(InternalSerializationApi::class)
  override suspend fun readFrom(input: InputStream): AppSettings {
    return try {
      val serializer = AppSettings::class.serializer()
      val string = input.readBytes().decodeToString()
      Json.decodeFromString(deserializer = serializer, string = string)
    } catch (e: Exception) {
      Timber.e(e)
      defaultValue
    }
  }

  @OptIn(InternalSerializationApi::class)
  @Suppress("BlockingMethodInNonBlockingContext")
  override suspend fun writeTo(t: AppSettings, output: OutputStream) {
    try {
      val serializer = AppSettings::class.serializer()
      output.write(Json.encodeToString(serializer = serializer, value = t).encodeToByteArray())
    } catch (e: Exception) {
      Timber.e(e)
    }
  }
}
