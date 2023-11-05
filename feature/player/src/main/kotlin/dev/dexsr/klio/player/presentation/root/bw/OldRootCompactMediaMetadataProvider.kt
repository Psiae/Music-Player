package dev.dexsr.klio.player.presentation.root.bw

import android.graphics.Bitmap
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.android.base.resource.AndroidLocalImage
import dev.dexsr.klio.player.presentation.LocalMediaArtwork
import dev.dexsr.klio.player.presentation.root.RootCompactMediaMetadataProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class OldRootCompactMediaMetadataProvider(
    private val user: User,
    private val viewModel: PlaybackControlViewModel
) : RootCompactMediaMetadataProvider {

    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun artworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> {
        return flow {
            val channel = Channel<LocalMediaArtwork?>(Channel.UNLIMITED)
            coroutineScope.launch(Dispatchers.Main) {
                viewModel.observeMediaArtwork(mediaID)
                    .map { art ->
                        if (art is Bitmap) {
                            return@map LocalMediaArtwork(
                                allowTransform = true,
                                image = AndroidLocalImage.Bitmap(art)
                            )
                        }
                        null
                    }
                    .collect {
                        channel.send(it)
                    }
            }
            for (art in channel) {
                emit(art)
            }
        }
    }

    fun dispose() {
        coroutineScope.cancel()
    }
}

