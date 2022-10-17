package com.flammky.mediaplayer.domain.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.app.AppDelegate
import com.flammky.android.environment.DeviceInfo
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.common.kotlin.collection.mutable.forEachClear
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.mediaplayer.helper.external.IntentWrapper
import com.flammky.mediaplayer.helper.external.MediaIntentHandler
import com.flammky.mediaplayer.helper.image.CoilHelper
import com.flammky.musicplayer.domain.musiclib.core.MusicLibrary
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemHelper
import com.flammky.musicplayer.ui.main.compose.screens.root.PlaybackControlModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
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

	private val cacheManager = AppDelegate.cacheManager

	private val ioScope = viewModelScope + dispatchers.io
	private val computationScope = viewModelScope + dispatchers.computation

	private val positionStateFlow = MusicLibrary.api.localAgent.session.info.playbackPosition
	private val bufferedPositionStateFlow = MusicLibrary.api.localAgent.session.info.playbackBufferedPosition
	private val itemStateFlow = MusicLibrary.api.localAgent.session.info.playbackItem

	private val player = MusicLibrary.api.localAgent.session.player

	private var positionCollectorJob = Job().job
	private var updateArtJob = Job().job

	val playbackControlModel = PlaybackControlModel()

	val pendingStorageIntent = mutableListOf<IntentWrapper>()
	init {
		viewModelScope.launch(dispatchers.main) {
			collectPlaybackState()
		}
		viewModelScope.launch(dispatchers.main) {
			positionStateFlow.safeCollect {
				Timber.d("positionStateFlow collected $it")
				playbackControlModel.updatePosition(it)
			}
		}
		viewModelScope.launch(dispatchers.main) {
			bufferedPositionStateFlow.safeCollect {
				playbackControlModel.updateBufferedPosition(it)
			}
		}
		viewModelScope.launch(dispatchers.main) {
			itemStateFlow.safeCollect {
				val get = playbackControlModel.actualMediaItem

				if (get != it) {
					playbackControlModel.updateArt(null)
					dispatchUpdateItemBitmap(it)
				}

				playbackControlModel.updateMediaItem(it)
			}
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
    viewModelScope.launch(dispatchers.computation) {
			if (intent.shouldHandleIntent) intentHandler.handleMediaIntent(intent)
		}
  }

  private suspend fun collectPlaybackState() {
    MusicLibrary.api.localAgent.session.info.playbackState.safeCollect { playbackState ->
			Timber.d("collectPlaybackState: $playbackState")
			playbackControlModel.updateBy(playbackState)
    }
  }

  suspend fun dispatchUpdateItemBitmap(item: com.flammky.android.medialib.common.mediaitem.MediaItem) {
		updateArtJob.cancel()
    updateArtJob = ioScope.launch {
			try {
				val req = ArtworkProvider.Request.Builder(item.mediaId, Bitmap::class.java)
					.setStoreMemoryCacheAllowed(true)
					.setDiskCacheAllowed(false)
					.build()
				val art = artworkProvider.request(req).await()
				ensureActive()
				playbackControlModel.updateArt(art.get())
			} catch (_: OutOfMemoryError) {}
		}
	}
}
