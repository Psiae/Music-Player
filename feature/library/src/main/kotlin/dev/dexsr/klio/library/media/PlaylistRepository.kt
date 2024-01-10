package dev.dexsr.klio.library.media

import dev.dexsr.klio.library.compose.Playlist
import kotlinx.coroutines.Deferred

interface PlaylistRepository {

	fun pagingMediator(): PlaylistPagingMediator

	fun fetchPlaylistInfoAsync(playlistId: String): Deferred<Result<Playlist>>

	fun dispose()
}
