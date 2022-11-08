package com.flammky.musicplayer.playbackcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.playbackcontrol.domain.model.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.domain.model.TrackInfo
import com.flammky.musicplayer.playbackcontrol.domain.usecase.PlaybackInfoUseCase
import com.flammky.musicplayer.playbackcontrol.domain.usecase.TrackInfoUseCase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

internal class PlaybackControlViewModel(
	private val coroutineDispatchers: AndroidCoroutineDispatchers,
	private val playbackInfoUseCase: PlaybackInfoUseCase,
	private val trackUseCase: TrackInfoUseCase
) : ViewModel() {
	private val _trackInfoChannelFlow = mutableMapOf<String, Flow<TrackInfo>>()

	val playbackInfoStateFlow = flow<PlaybackInfo> {
		emitAll(playbackInfoUseCase.observe())
	}.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackInfo.UNSET)

	// I think we should have this Flow channel in repository instead ?

	suspend fun observeTrackInfo(id: String): Flow<TrackInfo> = withContext(coroutineDispatchers.main) {
		_trackInfoChannelFlow[id]?.let { flow -> return@withContext flow }

		callbackFlow {
			trackUseCase.observe(id).collect(::send)
			awaitClose {
				_trackInfoChannelFlow.remove(id)
			}
		}.flowOn(coroutineDispatchers.main).also {
			if (_trackInfoChannelFlow.put(id, it) != null) {
				error("Duplicate TrackInfo Flow Channel")
			}
		}
	}
}
