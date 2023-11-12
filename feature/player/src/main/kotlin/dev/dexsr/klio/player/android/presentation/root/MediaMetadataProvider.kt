package dev.dexsr.klio.player.android.presentation.root

import dev.dexsr.klio.player.shared.LocalMediaArtwork
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface MediaMetadataProvider {

    fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?>

    fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?>
}

object NoOpMediaMetadataProvider : MediaMetadataProvider {

    override fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> = flowOf()

    override fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> = flowOf()
}