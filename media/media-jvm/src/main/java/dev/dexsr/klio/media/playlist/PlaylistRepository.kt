package dev.dexsr.klio.media.playlist

import dev.dexsr.klio.media.playlist.paging.PlaylistContentBucket
import dev.dexsr.klio.media.playlist.realm.PlaylistSynchronizeData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun observeChanges(
        playlistId: String
    ): Flow<Playlist>

    fun synchronizeOrCreate(
        synchronizeData: PlaylistSynchronizeData
    ): Deferred<Result<Playlist>>

    suspend fun getPagedPlaylistItems(
        playlistId: String,
        playlistSnapshotId: String,
        offset: Int,
        limit: Int
    ): Result<PlaylistContentBucket>

    fun dispose()
}