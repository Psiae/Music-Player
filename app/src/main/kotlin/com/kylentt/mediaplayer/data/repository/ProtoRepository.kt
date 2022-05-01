package com.kylentt.mediaplayer.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.app.settings.AppSettings
import com.kylentt.mediaplayer.app.settings.AppSettingsSerializer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Repository Containing Protobuf related data, e.g: AppSettings in Proto DataStore
 * @property [appSettingSF] @returns [StateFlow] of [AppSettings] from [collectFromSettings]
 * @property [collectFromSettings] @returns [Flow] of [AppSettings] from [androidx.datastore.core.DataStore]
 * @property [writeToSettings] Writes [AppSettings] to [androidx.datastore.core.DataStore]
 * @see ProtoRepositoryImpl
 * @author Kylentt
 * @since 2022/04/30
 */



interface ProtoRepository {
  val appSettingSF: StateFlow<AppSettings>
  suspend fun collectFromSettings(): Flow<AppSettings>
  suspend fun writeToSettings(data: suspend (AppSettings) -> AppSettings)
}

class ProtoRepositoryImpl(
  private val context: Context,
  private val scope: AppScope
): ProtoRepository {

  companion object {
    @JvmStatic
    val Context.settingsDataStore by dataStore("app-settings.json", AppSettingsSerializer)
  }

  private val _appSettings = MutableStateFlow(AppSettings.INVALID)

  override val appSettingSF: StateFlow<AppSettings>
    get() = _appSettings.asStateFlow()

  override suspend fun collectFromSettings(): Flow<AppSettings> {
    return context.settingsDataStore.data
  }

  override suspend fun writeToSettings(data: suspend (AppSettings) -> AppSettings) {
    try {
      context.settingsDataStore.updateData { data(it) }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  init {
    require(context is Application) { "Invalid Context" }
    scope.ioScope.launch {
      collectFromSettings().collect { _appSettings.value = it }
    }
  }
}
