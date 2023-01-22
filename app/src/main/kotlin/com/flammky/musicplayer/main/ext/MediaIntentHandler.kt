package com.flammky.musicplayer.main.ext

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.flammky.android.content.intent.isActionView
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.common.kotlin.coroutines.AutoCancelJob
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileNotFoundException
import kotlin.coroutines.coroutineContext


class MediaIntentHandler constructor(
	private val presenter: Presenter
) {

	private var jobSlot: Job by AutoCancelJob()

	fun isMediaIntent(intent: Intent): Boolean {
		if (!intent.isActionView()) {
			return false
		}

		val data = intent.data
			?: return false

		val type = intent.type
			?: return false

		@Suppress("SimplifyBooleanWithConstants")
		val schemeSupported = data.scheme == ContentResolver.SCHEME_CONTENT
			/* || data.scheme == ContentResolver.SCHEME_FILE */

		return schemeSupported && type.startsWith("audio/")
	}

	fun handleMediaIntent(intent: Intent) {
		val clone = intent.clone() as Intent
		if (!isMediaIntent(clone)) return
		jobSlot = presenter.coroutineScope.launch(presenter.coroutineDispatchers.io) {
			val data = clone.data!!
			val id = data.toString()
			if (!validateAudioURI(data)) {
				return@launch
			}
			ensureActive()
			presenter.playbackConnection
				.requestUserSessionAsync(presenter.authService.currentUser ?: return@launch).await()
				.controller.withLooperContext {
					ensureActive()
					setQueue(OldPlaybackQueue(persistentListOf(id), 0))
					play()
				}
		}
	}

	private suspend fun validateAudioURI(uri: Uri): Boolean {
		// check if we can open the uri
		runCatching {
			presenter.androidContext.contentResolver.openInputStream(uri)!!.close()
		}.exceptionOrNull()?.let { ex ->
			coroutineContext.ensureActive()
			when (ex) {
				is FileNotFoundException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File is not found"
				)
				is SecurityException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File could not be read (no provider permission)"
				)
				is NullPointerException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File could not be read (inaccessible provider)"
				)
				else -> presenter.showIntentRequestErrorMessage(message = "Unexpected Error Occurred")
			}.also {
				Timber.d("Exception on validateAudioURI($uri), ex: $ex")
			}
			return false
		}

		return true
	}

	interface Presenter {
		val authService: AuthService
		val androidContext: Context
		val artworkProvider: ArtworkProvider
		val coroutineDispatchers: AndroidCoroutineDispatchers
		val coroutineScope: CoroutineScope
		val playbackConnection: PlaybackConnection
		val sharedRepository: MediaMetadataCacheRepository
		val mediaStore: MediaStoreProvider

		fun showIntentRequestErrorMessage(message: String)
	}
}
