package com.flammky.musicplayer.library.dump.media

import android.content.Context
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.android.base.Playback
import com.flammky.musicplayer.base.media.MediaConstants
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.user.User
import dev.dexsr.klio.library.playback.PlaylistPlaybackInfo
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber

@Deprecated("Redo")
internal class RealMediaConnection(
    private val artworkProvider: ArtworkProvider,
    private val context: Context,
    private val dispatcher: AndroidCoroutineDispatchers,
    private val mediaStore: MediaStoreProvider,
    private val mediaRepo: MediaMetadataCacheRepository,
    private val playbackConnection: PlaybackConnection
) : MediaConnection {
	private val coroutineScope = CoroutineScope(SupervisorJob())

	override fun play(
		user: User,
		queue: List<Pair<String, Uri>>,
		index: Int
	) {
		Timber.d(
			"""
				RMC play:
				queue=$queue
				index=$index
			"""
		)
		if (index !in queue.indices) return
		coroutineScope.launch(Playback.DISPATCHER) {
			playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
				val mappedQueue = queue.map { it.first }.toPersistentList()
				setQueue(OldPlaybackQueue(mappedQueue, index))
				play()
			}
		}
	}

	override fun observePlaylistPlaybackInfo(
		user: User
	): Flow<PlaylistPlaybackInfo> {

		return flow {
			val channel = Channel<PlaylistPlaybackInfo>(Channel.CONFLATED)
			var latest = PlaylistPlaybackInfo.UNSET

			try {
				coroutineScope.launch(Playback.DISPATCHER) {
					playbackConnection
						.requestUserSessionAsync(user)
						.await()
						.controller.withLooperContext {
							val owner = Any()
							val obs = acquireObserver(owner)

							try {
								obs.observeIsPlaying().collect {
									val new = latest.copy(playing = it, canPause = true, canPlay = true)
									latest = new
									channel.send(new)
								}
							} finally {
								releaseObserver(owner)
							}
						}
				}
				for (e in channel) { emit(e) }
			} finally {
			    channel.close()
			}
		}
	}

	override val repository: MediaConnection.Repository = object : MediaConnection.Repository {



		override fun getArtwork(id: String): Any? {
			return mediaRepo.getArtwork(id)
		}

		// we should localize
		override suspend fun observeArtwork(id: String): Flow<Any?> {
			return mediaRepo.observeArtwork(id)
		}

		override suspend fun provideArtwork(id: String, artwork: Any?, silent: Boolean) {
			if (silent) {
				mediaRepo.silentProvideArtwork(id, artwork ?: MediaConstants.NO_ARTWORK)
			} else {
				mediaRepo.provideArtwork(id, artwork ?: MediaConstants.NO_ARTWORK)
			}
		}

		override suspend fun evictArtwork(id: String, silent: Boolean) {
			if (silent) {
				mediaRepo.silentEvictArtwork(id)
			} else {
				mediaRepo.evictArtwork(id)
			}
		}

		override fun getMetadata(id: String): MediaMetadata? {
			return mediaRepo.getMetadata(id)
		}

		override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> {
			return mediaRepo.observeMetadata(id)
		}

		override suspend fun provideMetadata(id: String, metadata: MediaMetadata) {
			return mediaRepo.provideMetadata(id, metadata)
		}

		override suspend fun evictMetadata(id: String, silent: Boolean) {
			if (silent) {
				mediaRepo.silentEvictMetadata(id)
			} else {
				mediaRepo.evictMetadata(id)
			}
		}
	}
}
