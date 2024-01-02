package dev.dexsr.klio.media

import dev.dexsr.klio.media.playlist.PlaylistRepository

interface MediaRepository {

    val playlist: PlaylistRepository
}