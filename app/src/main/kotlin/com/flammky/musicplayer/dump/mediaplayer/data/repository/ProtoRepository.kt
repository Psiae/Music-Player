package com.flammky.musicplayer.dump.mediaplayer.data.repository

import android.app.Application
import android.content.Context
import com.flammky.android.app.coroutines.AppScope
import com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings.Companion.settingsDataStore
import com.flammky.musicplayer.dump.mediaplayer.helper.Preconditions.checkArgument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Singleton

/**
 * Proto Datastore Repository
 *
 * @see ProtoRepositoryImpl
 * @author Kylentt
 * @since 2022/04/30
 */


interface ProtoRepository {
	val appSettingSF: StateFlow<com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings>
	suspend fun collectFromSettings(): Flow<com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings>
	suspend fun writeToSettings(data: suspend (com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings) -> com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings)
}

@Singleton
class ProtoRepositoryImpl(
	private val context: Context,
	private val scope: AppScope
) : ProtoRepository {

	private val _appSettings =
		MutableStateFlow(com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings.INVALID)

	override val appSettingSF: StateFlow<com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings>
		get() = _appSettings.asStateFlow()

	override suspend fun collectFromSettings(): Flow<com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings> {
		return context.settingsDataStore.data
	}

	override suspend fun writeToSettings(data: suspend (com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings) -> com.flammky.musicplayer.dump.mediaplayer.core.app.settings.AppSettings) {
		try {
			context.settingsDataStore.updateData { data(it) }
		} catch (e: Exception) {
			Timber.e(e)
		}
	}

	init {
		checkArgument(context is Application) {
			"Singleton Context must be Application"
		}
		scope.ioScope.launch {
			collectFromSettings().collect { _appSettings.value = it }
		}
	}
}
