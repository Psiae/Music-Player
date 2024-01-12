package dev.dexsr.klio.library.user.playlist

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import dev.dexsr.klio.base.breakLoop
import dev.dexsr.klio.base.continueLoop
import dev.dexsr.klio.base.strictResultingLoop
import dev.dexsr.klio.core.AndroidUiFoundation
import dev.dexsr.klio.core.isOnUiThread
import dev.dexsr.klio.library.compose.ComposeImmutable
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
import kotlin.math.min

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
				return coroutineScope.async(Dispatchers.IO) { runCatching {
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

	private var _workers = mutableMapOf<Int, Job>()
	private var _buckets = mutableListOf<PageBucket>()

	var renderData by mutableStateOf<RenderData?>(null)
		private set

	@UiThread
	fun init() {
		check(AndroidUiFoundation.isOnUiThread())
		check(init == null)
		_coroutineScope = CoroutineScope(SupervisorJob())
		_pagingMediator = repository.pagingMediator()
		init = coroutineScope.launch(Dispatchers.Main) {
			val playlistInfo = strictResultingLoop<Playlist> {
				repository
					.fetchPlaylistInfoAsync(playlistId)
					.await()
					.fold(
						onSuccess = { data -> this breakLoop data },
						// fixme: ask user to refresh
						onFailure = { this continueLoop delay(Long.MAX_VALUE) }
					)
			}
			renderData = playlistInfo.toRenderData(
				batchIntervalSize,
				intentLoadToOffset = { intentLoadToOffset(it, playlistInfo.snapshotId ) }
			)
		}
	}

	fun dispose() {
		_coroutineScope?.cancel()
		repository.dispose()
	}

	@AnyThread
	private fun intentLoadToOffset(
		offset: Int,
		snapshotId: String
	) {
		if (offset < 0) return
		// allow for undispatched execute
		coroutineScope.launch(Dispatchers.Main.immediate) {
			val segment = offset / batchIntervalSize
			val worker = _workers[segment]
			// join ongoing fetch
			if (worker?.isActive == true) return@launch
			if (renderData?.peekContent(offset) != null) return@launch
			_workers[segment] = dispatchIntentLoadData(segment, snapshotId)
		}
	}

	private fun dispatchIntentLoadData(
		segment: Int,
		snapshotId: String
	): Job {
		return coroutineScope.launch(Dispatchers.Main) {
			/*delay(2000)*/
			val result = strictResultingLoop<PagedPlaylistData> {
				pagingMediator
					.fetchNextAsync(snapshotId, batchIntervalSize * segment, batchIntervalSize)
					.await()
					.fold(
						onSuccess = { data -> this breakLoop data },
						// fixme: ask user to refresh
						onFailure = { this continueLoop delay(Long.MAX_VALUE) }
					)
			}
			updateSegment(segment, result)
		}
	}

	@MainThread
	private fun updateSegment(
		segment: Int,
		data: PagedPlaylistData
	) {
		val firstIndex = segment * batchIntervalSize
		val lastIndex = firstIndex + data.data.lastIndex
		// check if the data size is valid
		check(lastIndex - firstIndex < batchIntervalSize) {
			"updateSegment fail, lastIndex=$lastIndex, firstIndex=$firstIndex, segment=$segment"
		}
		var bucketToJoinIndex = -1
		val bucketToJoin = _buckets.fastFirstOrNull {
			bucketToJoinIndex++
			it.pageFirstIndex == lastIndex + 1 || it.pageLastIndex == firstIndex - 1
		}
		if (bucketToJoin != null) {
			val bucket = PageBucket(
				pages = mutableListOf<PagedPlaylistData>()
					.apply {
						if (lastIndex < bucketToJoin.pageFirstIndex) {
							add(data)
						}
						bucketToJoin.appendToPage(this)
						if (firstIndex > bucketToJoin.pageLastIndex) {
							add(data)
						}
					},
				pageFirstIndex = min(firstIndex, bucketToJoin.pageFirstIndex),
				pageSize = batchIntervalSize
			)
			_buckets.removeAt(bucketToJoinIndex)
			_buckets.add(bucketToJoinIndex, bucket)
		} else {
			val bucket = PageBucket(
				pages = listOf(data),
				pageFirstIndex = data.offset,
				pageSize = batchIntervalSize
			)
			var indexToPlace = -1
			_buckets.fastFirstOrNull {
				indexToPlace++
				it.pageFirstIndex > bucket.pageLastIndex
			}
			_buckets.add(indexToPlace.coerceAtLeast(0), bucket)
		}
		onSegmentUpdated()
	}

	private fun onSegmentUpdated(

	) {
		val currentRenderData = renderData
		val firstBucket = _buckets.firstOrNull()
		val lastBucket = _buckets.lastOrNull()
		renderData = RenderData(
			playlistId = playlistId,
			playlistSnapshotId = currentRenderData?.playlistSnapshotId ?: "",
			playlistTotalTrack = currentRenderData?.playlistTotalTrack ?: -1,
			contentCount = _buckets.sumOf { it.pageTotal },
			contentFirstIndex = firstBucket?.pageFirstIndex ?: -1,
			contentLastIndex = lastBucket?.pageLastIndex ?: -1,
			contentPageSize = batchIntervalSize,
			buckets = ArrayList(_buckets),
			// fixme
			intentLoadToOffset = currentRenderData?.intentLoadToOffset ?: {}
		)
		println("onSegmentUpdated, contentCount=${renderData?.contentCount}, fi=${renderData?.contentFirstIndex}, li=${renderData?.contentLastIndex}, buckets=${renderData?.buckets}")
	}

	class RenderData(
		val playlistId: String,
		val playlistSnapshotId: String,
		val playlistTotalTrack: Int,
		val contentCount: Int,
		val contentFirstIndex: Int,
		val contentLastIndex: Int,
		val contentPageSize: Int,
		val buckets: List<PageBucket>,
		val intentLoadToOffset: (offset: Int) -> Unit
	)

	// fetching is not necessarily ordered, bucket has no skips
	// TODO: for now assume that every page is of same size and that they are all full expect last page
	@ComposeImmutable
	class PageBucket(
		private val pages: List<PagedPlaylistData>,
		val pageFirstIndex: Int,
		val pageSize: Int
	) {

		val pageTotal = pages.sumOf { it.data.size }

		val pageLastIndex: Int
			get() = pageFirstIndex + pageTotal - 1

		fun getByRawIndex(index: Int): String? {
			if (index < pageFirstIndex || index > pageLastIndex) return null
			val relativeIndex = index - pageFirstIndex
			val segment = relativeIndex / pageSize
			return pages[segment].data[relativeIndex - pageSize * segment]
		}

		fun appendToPage(list: MutableList<PagedPlaylistData>) = list.addAll(pages)
	}

	companion object {
		const val BATCH_DEFAULT_INTERVAL_SIZE = 20
	}
}

@AnyThread
fun PlaylistDetailLazyLayoutState.RenderData.peekContent(index: Int): String? {
	if (index < 0) throw IndexOutOfBoundsException("cannot access index=$index")
	if (buckets.isEmpty()) return null
	val firstBucket = buckets.first()
	if (index < firstBucket.pageFirstIndex) return null
	val lastBucket = buckets.last()
	if (index > lastBucket.pageLastIndex) return null
	// array backed
	buckets.fastForEach { bucket -> bucket.getByRawIndex(index)?.let { return it } }
	return null
}

@AnyThread
fun PlaylistDetailLazyLayoutState.RenderData.getContent(index: Int): String? {
	if (index < 0) throw IndexOutOfBoundsException("cannot access index=$index")
	if (buckets.isEmpty() ||
		index < buckets.first().pageFirstIndex ||
		index > buckets.last().pageLastIndex
	) {
		intentLoadToOffset(index)
		return null
	}
	// array backed
	buckets.fastForEach { bucket -> bucket.getByRawIndex(index)?.let { return it } }
	intentLoadToOffset(index)
	return null
}

private fun Playlist.toRenderData(
	pageSize: Int,
	intentLoadToOffset: (offset: Int) -> Unit
): PlaylistDetailLazyLayoutState.RenderData {
	return PlaylistDetailLazyLayoutState.RenderData(
		playlistId = id,
		playlistSnapshotId = snapshotId,
		playlistTotalTrack = contentCount,
		contentCount = 0,
		contentFirstIndex = -1,
		contentLastIndex = -1,
		contentPageSize = pageSize,
		buckets = emptyList(),
		intentLoadToOffset = intentLoadToOffset
	)
}
