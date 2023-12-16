package dev.dexsr.klio.player.android.presentation.root.bw

import android.graphics.Bitmap
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.android.base.resource.AndroidLocalImage
import dev.dexsr.klio.player.shared.LocalMediaArtwork
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import dev.dexsr.klio.player.android.presentation.root.main.RootCompactMediaMetadataProvider
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
            val observer = coroutineScope.launch(Dispatchers.Main) {
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
            try {
                for (art in channel) {
                    emit(art)
                }
            } finally {
                observer.cancel()
            }
        }
    }

    override fun descriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> {
        return flow {
            val channel = Channel<PlaybackMediaDescription>(Channel.UNLIMITED)
            val observer = coroutineScope.launch(Dispatchers.Main) {
                viewModel.observeMediaMetadata(mediaID)
                    .map { metadata ->
                        PlaybackMediaDescription(
                            metadata?.findTitle(),
                            metadata?.findSubtitle()
                        )
                    }
                    .collect {
                        channel.send(it)
                    }
            }
            try {
                for (element in channel) {
                    emit(element)
                }
            } finally {
                observer.cancel()
            }
        }
    }

    private fun MediaMetadata.findTitle(): String? = title?.ifBlank { null }
    private fun MediaMetadata.findSubtitle(): String? = (this as? AudioMetadata)
        ?.let {
            it.albumArtistName ?: it.artistName
        }
        ?: (this as? AudioFileMetadata)?.file
            ?.let { fileMetadata ->
                fileMetadata.fileName?.ifBlank { null }
                    ?: (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
            }
            ?.ifBlank { null }

    fun dispose() {
        coroutineScope.cancel()
    }
}

