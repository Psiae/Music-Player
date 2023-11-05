package dev.dexsr.klio.player.presentation.root

import dev.dexsr.klio.player.presentation.LocalMediaArtwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface RootCompactMediaMetadataProvider {

    fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?>
}

object NoOpRootCompactMediaMetadataProvider : RootCompactMediaMetadataProvider {

    override fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> = flowOf()
}
