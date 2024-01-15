package dev.dexsr.klio.library.media

import dev.dexsr.klio.library.compose.PlaylistInfo
import kotlinx.coroutines.Deferred

interface PlaylistRepository {

	fun pagingMediator(playlistId: String): PlaylistPagingMediator

	fun metadataProvider(playlistId: String): PlaylistMetadataProvider

	fun fetchPlaylistInfoAsync(playlistId: String): Deferred<Result<PlaylistInfo>>
}

interface DisposablePlaylistRepository : PlaylistRepository {

	fun dispose()
}
