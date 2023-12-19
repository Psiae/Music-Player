package dev.dexsr.klio.player.android.presentation.root.main

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import dev.dexsr.klio.player.shared.LocalMediaArtwork
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface MediaMetadataProvider {

    fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?>

    fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?>

    fun oldDescriptionAsFlow(mediaID: String): Flow<MediaMetadata?>
}

object NoOpMediaMetadataProvider : MediaMetadataProvider {

    override fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> = flowOf()

    override fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> = flowOf()

    override fun oldDescriptionAsFlow(mediaID: String): Flow<MediaMetadata> = flowOf()
}