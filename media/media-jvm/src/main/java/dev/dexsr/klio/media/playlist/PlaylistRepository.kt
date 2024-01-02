package dev.dexsr.klio.media.playlist

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    fun observeChanges(
        playlistId: String
    ): Flow<Playlist>

    fun update(
        new: Playlist
    ): Deferred<Result<Playlist>>

    fun create(
        new: Playlist
    ): Deferred<Result<Playlist>>

    fun updateOrCreate(
        new: Playlist
    ): Deferred<Result<Playlist>>

    fun updateOrCreate(
        playlistId: String,
        update: suspend () -> Playlist
    ): Deferred<Result<Playlist>>

    fun get(id: String): Deferred<Result<Playlist?>>

    fun getOrCreate(
        id: String,
        create: suspend () -> Playlist
    ): Deferred<Result<Playlist>>

    fun dispose()
}