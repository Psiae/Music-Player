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
 * Repository Containing Protobuf related data.
 * e.g: AppSettings in Proto DataStore
 * */

val Context.settingsDataStore by dataStore("app-settings.json", AppSettingsSerializer)

interface ProtoRepository {
  val appSettingSF: StateFlow<AppSettings>
  suspend fun collectFromSettings(): Flow<AppSettings>
  suspend fun writeToSettings(data: suspend (AppSettings) -> AppSettings)
}

class ProtoRepositoryImpl(
  val context: Context,
  val scope: AppScope
): ProtoRepository {

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
