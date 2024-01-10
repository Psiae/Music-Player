package dev.dexsr.klio.library.user.playlist

import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.dexsr.klio.base.strictResultingLoop
import dev.dexsr.klio.core.AndroidUiFoundation
import dev.dexsr.klio.core.isOnUiThread
import dev.dexsr.klio.library.compose.ComposeStable
import dev.dexsr.klio.library.compose.PagedPlaylistData
import dev.dexsr.klio.library.compose.Playlist
import dev.dexsr.klio.library.compose.toStablePlaylist
import dev.dexsr.klio.library.media.PlaylistPagingMediator
import dev.dexsr.klio.library.media.PlaylistRepository
import dev.dexsr.klio.library.media.RealPlaylistPagingMediator
import dev.dexsr.klio.media.playlist.LocalPlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun rememberPlaylistDetailLazyLayoutState(
	playlistId: String,
	orderedMeasure: Boolean,
	playlistRepository: PlaylistRepository = /*runtimeInject()*/ remember {
		// TODO
		object : PlaylistRepository {
			val localRepo = LocalPlaylistRepository()
			val coroutineScope = CoroutineScope(SupervisorJob())
			override fun pagingMediator(): PlaylistPagingMediator {
				return RealPlaylistPagingMediator(localRepo, playlistId)
			}
			override fun fetchPlaylistInfoAsync(playlistId: String): Deferred<Result<Playlist>> {
				return coroutineScope.async { runCatching {
					localRepo.observeChanges(playlistId).first().toStablePlaylist()
				} }
			}
			override fun dispose() {
				localRepo.dispose()
				coroutineScope.cancel()
			}
		}
	}
): PlaylistDetailLazyLayoutState {
	return remember(playlistId, orderedMeasure) {
		PlaylistDetailLazyLayoutState(playlistId, orderedMeasure, playlistRepository)
	}.apply {
		DisposableEffect(key1 = this, effect = {
			init()
			onDispose(this@apply::dispose)
		})
	}
}

@ComposeStable
class PlaylistDetailLazyLayoutState(
	private val playlistId: String,
	private val orderedMeasure: Boolean,
	private val repository: PlaylistRepository,
	private val batchIntervalSize: Int = BATCH_DEFAULT_INTERVAL_SIZE
) {

	init {
	    check(batchIntervalSize > 0) {
			"batchIntervalSize must be at least 1"
		}
	}

	private var _coroutineScope: CoroutineScope? = null
	private var _pagingMediator: PlaylistPagingMediator? = null

	private val coroutineScope
		get() = checkNotNull(_coroutineScope) {
			"coroutineScope wasn't initialized, make sure to call init"
		}

	private val pagingMediator
		get() = checkNotNull(_pagingMediator) {
			"pagingMediator wasn't initialized, make sure to call init"
		}

	private var init: Job? = null

	var data by mutableStateOf<RenderData?>(null)

	@UiThread
	fun init() {
		check(AndroidUiFoundation.isOnUiThread())
		check(init == null)
		_coroutineScope = CoroutineScope(SupervisorJob())
		_pagingMediator = repository.pagingMediator()
		init = coroutineScope.launch(Dispatchers.Main) {
			val playlistInfo = strictResultingLoop<Playlist> {
				repository.fetchPlaylistInfoAsync(playlistId).await().fold(
					onSuccess = { LOOP_BREAK(it) },
					// fixme: ask user to refresh
					onFailure = { delay(Long.MAX_VALUE) ; LOOP_CONTINUE() }
				)
			}
			val firstBatch = strictResultingLoop<PagedPlaylistData> {
				pagingMediator
					.fetchNextAsync(playlistInfo.snapshotId, 0)
					.await()
					.fold(
						onSuccess = { LOOP_BREAK(it) },
						// fixme: ask user to refresh
						onFailure = { delay(Long.MAX_VALUE) ; LOOP_CONTINUE() }
					)
			}
			data = firstBatch
				.let { RenderData(it.id, it.snapshotId, it.data.size, it.pageMaxSize, listOf(it)) }
		}
	}

	fun dispose() {
		_coroutineScope?.cancel()
		repository.dispose()
	}

	class RenderData(
		val playlistId: String,
		val playlistSnapshotId: String,
		val contentCount: Int,
		val contentPageSize: Int,
		val contentPages: List<PagedPlaylistData>
	)

	companion object {
		const val BATCH_DEFAULT_INTERVAL_SIZE = 20
	}
}

fun PlaylistDetailLazyLayoutState.RenderData.getContent(index: Int): String {
	// round down
	val segment = index / contentPageSize
	val segmentFirstIndex = segment * contentPageSize
	return contentPages[segment].data[index - segmentFirstIndex]
}
