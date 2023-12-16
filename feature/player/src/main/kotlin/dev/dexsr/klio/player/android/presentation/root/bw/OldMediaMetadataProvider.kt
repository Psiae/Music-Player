package dev.dexsr.klio.player.android.presentation.root.bw

import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.player.android.presentation.root.main.MediaMetadataProvider
import dev.dexsr.klio.player.shared.LocalMediaArtwork
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import kotlinx.coroutines.flow.Flow

internal class OldMediaMetadataProvider(
    user: User,
    vm: PlaybackControlViewModel
) : MediaMetadataProvider {

    // same impl, going to be removed anyway
    private val compact = OldRootCompactMediaMetadataProvider(
        user = user,
        viewModel = vm
    )

    override fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> {
        return compact.artworkAsFlow(mediaID)
    }

    override fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> {
        return compact.descriptionAsFlow(mediaID)
    }

    fun dispose() {
        compact.dispose()
    }
}