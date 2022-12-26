package com.flammky.musicplayer.dump.mediaplayer.core.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings.Companion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Class to store the Application Settings, [kotlinx.serialization.Serializable] for [androidx.datastore.core.DataStore]
 * @constructor [navigationSettings] the App Navigation Settings
 * @constructor [wallpaperSettings] the App Wallpaper Settings
 * @constructor [isValid] whether the instance is a Valid [AppSettings] or a Placeholder
 * @property [Companion.DEFAULT] Static Default Instance
 * @property [Companion.INVALID] Static Invalid Instance
 * @see [AppSettingsSerializer]
 * @author Kylentt
 * @since 2022/04/30
 */

@Serializable
data class AppSettings(
	val navigationSettings: NavigationSettings,
	val wallpaperSettings: WallpaperSettings,
	val isValid: Boolean = true
) {

	val defaultValue
		get() = DEFAULT
	val fileName
		get() = Companion.fileName

	fun getDataStore(context: Context): DataStore<AppSettings> =
		context.settingsDataStore

	companion object {
		const val fileName = "app-settings.json"

		@JvmStatic
		val DEFAULT: AppSettings by lazy {
			val navigation =
				NavigationSettings.DEFAULT
			val wallpaper =
				WallpaperSettings.DEFAULT
			AppSettings(
				navigationSettings = navigation,
				wallpaperSettings = wallpaper
			)
		}

		@JvmStatic
		val INVALID =
			DEFAULT.copy(
				isValid = false
			)

		@JvmStatic
		val Context.settingsDataStore by dataStore(
			fileName,
			AppSettingsSerializer
		)
	}
}

object AppSettingsSerializer :
	Serializer<AppSettings> {

	override val defaultValue: AppSettings
		get() = Companion.DEFAULT

	@OptIn(InternalSerializationApi::class)
	override suspend fun readFrom(input: InputStream): AppSettings =
		withContext(Dispatchers.IO) {
			try {
				val serializer =
					AppSettings::class.serializer()
				val string = input.readBytes().decodeToString()
				Json.decodeFromString(deserializer = serializer, string = string)
			} catch (e: Exception) {
				Timber.e(e)
				defaultValue
			}
		}

	@OptIn(InternalSerializationApi::class)
	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun writeTo(
		t: AppSettings,
		output: OutputStream
	) =
		withContext(Dispatchers.IO) {
			try {
				val serializer =
					AppSettings::class.serializer()
				output.write(Json.encodeToString(serializer = serializer, value = t).encodeToByteArray())
			} catch (e: Exception) {
				Timber.e(e)
			}
		}
}
