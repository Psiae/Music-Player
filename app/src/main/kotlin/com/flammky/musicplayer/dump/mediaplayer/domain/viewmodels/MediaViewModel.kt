package com.flammky.musicplayer.dump.mediaplayer.domain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.environment.DeviceInfo
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.common.kotlin.collection.mutable.forEachClear
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.domain.musiclib.core.MusicLibrary
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemHelper
import com.flammky.musicplayer.dump.mediaplayer.helper.external.IntentWrapper
import com.flammky.musicplayer.dump.mediaplayer.helper.external.MediaIntentHandler
import com.flammky.musicplayer.dump.mediaplayer.helper.image.CoilHelper
import com.flammky.musicplayer.ui.main.compose.screens.root.PlaybackControlModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
	private val coilHelper: CoilHelper,
	private val deviceInfo: DeviceInfo,
	private val dispatchers: com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers,
	private val itemHelper: MediaItemHelper,
	private val intentHandler: MediaIntentHandler,
	private val artworkProvider: ArtworkProvider,
) : ViewModel() {
	private val ioScope = viewModelScope + dispatchers.io
	private val player = MusicLibrary.api.localAgent.session.player

	private var updateArtJob = Job().job

	val playbackControlModel = PlaybackControlModel()

	val pendingStorageIntent = mutableListOf<IntentWrapper>()
	init {
		viewModelScope.launch(dispatchers.main) {
			collectPlaybackState()
		}
	}

	fun readPermissionGranted() {
		pendingStorageIntent.forEachClear(::handleMediaIntent)
	}

	fun play() {
		player.joinBlockingSuspend {
			if (!player.connected) {
				player.connectService().await()
			}
			when {
				playbackState.isIDLE() -> player.prepare()
				playbackState.isENDED() -> player.seekToDefaultPosition()
			}
			play()
		}
	}


	fun pause() {
		viewModelScope.launch { player.pause() }
	}

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch(dispatchers.io) {
			if (intent.shouldHandleIntent) intentHandler.handleMediaIntentI(intent)
		}
  }

  private suspend fun collectPlaybackState() {
    MusicLibrary.api.localAgent.session.info.playbackState.safeCollect { playbackState ->
			Timber.d("collectPlaybackState: $playbackState")
			playbackControlModel.updateBy(playbackState)
    }
  }
}
