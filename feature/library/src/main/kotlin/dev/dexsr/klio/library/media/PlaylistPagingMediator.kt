package dev.dexsr.klio.library.media

import dev.dexsr.klio.base.ktx.coroutines.initAsParentCompleter
import dev.dexsr.klio.library.compose.PagedPlaylistData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import dev.dexsr.klio.media.playlist.PlaylistRepository as MediaPlaylistRepository

interface PlaylistPagingMediator {

	fun fetchNextAsync(
		playlistSnapshotId: String,
		currentOffset: Int,
		limit: Int
	): Deferred<Result<PagedPlaylistData>>

	fun dispose()
}


internal class RealPlaylistPagingMediator(
	private val playlistRepository: MediaPlaylistRepository,
	private val playlistId: String,
) : PlaylistPagingMediator {

	private val coroutineScope = CoroutineScope(SupervisorJob())

	override fun fetchNextAsync(
		playlistSnapshotId: String,
		currentOffset: Int,
		limit: Int
	): Deferred<Result<PagedPlaylistData>> {
		val def = CompletableDeferred<Result<PagedPlaylistData>>()

		coroutineScope.launch(Dispatchers.IO) {

			def.complete(
				runCatching {
					val data = playlistRepository
						.getPagedPlaylistItems(playlistId, playlistSnapshotId, currentOffset, limit)
						.getOrThrow()
					PagedPlaylistData(data.playlistId, data.playlistSnapshotId, currentOffset, data.contents, limit)
				}
			)

		}.initAsParentCompleter(def)

		return def
	}

	override fun dispose() {
		coroutineScope.cancel()
	}
}
