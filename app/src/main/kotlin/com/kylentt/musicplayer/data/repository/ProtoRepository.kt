package com.kylentt.musicplayer.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.dataStore
import com.kylentt.musicplayer.app.util.AppScope
import com.kylentt.musicplayer.ui.preferences.AppSettings
import com.kylentt.musicplayer.ui.preferences.AppSettingsSerializer
import com.kylentt.musicplayer.ui.preferences.AppState
import com.kylentt.musicplayer.ui.preferences.AppStateSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

val Context.settingsDataStore by dataStore("app-settings.json", AppSettingsSerializer)
val Context.stateDataStore by dataStore("app-state.json", AppStateSerializer)

class ProtoRepository(
    val context: Context,
    val scope: AppScope
) {

    private val _appState = MutableStateFlow<AppState>(AppState.Defaults.INVALID)
    private val _appSettings = MutableStateFlow<AppSettings>(AppSettings.Defaults.invalid)

    val appState = _appState.asStateFlow()
    val appSettings = _appSettings.asStateFlow()

    init {
        check(context is Application) { "Invalid Context" }
        scope.ioScope.launch { collectState().collect { _appState.value = it } }
        scope.ioScope.launch { collectSettings().collect { _appSettings.value = it } }
    }

    suspend fun writeToSettings(data: suspend (AppSettings) -> AppSettings) {
        try {
            context.settingsDataStore.updateData { data(it) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    suspend fun writeToState(data: suspend (current: AppState) -> AppState) {
        try {
            context.stateDataStore.updateData { data(it) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun collectSettings(): Flow<AppSettings> {
        return context.settingsDataStore.data
    }

    fun collectState(): Flow<AppState> {
        return context.stateDataStore.data
    }


}